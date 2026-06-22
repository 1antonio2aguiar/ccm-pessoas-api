package br.com.cc.pessoas.unificacao.saneamento.saneService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaneCargaPessoaCnpjJaExisteCadUnicoService {

    @PersistenceContext
    private EntityManager manager;

    private final SaneCargaPessoaHelperService helper;

    @Transactional
    public String processarCnpjJaExisteCadUnico(Long pessoaId) {

        if (pessoaId == null) {
            throw new IllegalArgumentException("Código da pessoa do saneamento não informado.");
        }

        Long existeVinculoSane = helper.count("""
            select count(1)
              from dbo_ccm_pessoas.cad_unico_pessoa cup
             where cup.cd_origem = :pessoaId
               and cup.banco = 'SANE'
        """, "pessoaId", pessoaId);

        if (existeVinculoSane > 0) {
            return "Pessoa jurídica do saneamento já vinculada ao Cadastro Único.";
        }

        Object[] sane = buscarPessoaSane(pessoaId);

        Long pessoasCdUnico = localizarPessoaJuridicaCadUnicoExistente(sane);

        if (pessoasCdUnico == null) {
            throw new IllegalArgumentException("Pessoa jurídica existente no Cadastro Único não localizada para o SANE: " + pessoaId);
        }

        inserirVinculoCadUnicoPessoa(pessoasCdUnico, sane);
        complementarDadosPj(pessoasCdUnico, sane);
        complementarContatos(pessoasCdUnico, sane);
        complementarEnderecos(pessoasCdUnico, sane);

        return "Pessoa jurídica SANE vinculada ao Cadastro Único existente. Código único: " + pessoasCdUnico + ".";
    }
    private void complementarEnderecos(Long pessoasCdUnico, Object[] sane) {

        helper.inserirEnderecosUnicosSane(
                pessoasCdUnico,
                java.util.Collections.singletonList(sane)
        );
    }
    private void complementarContatos(Long pessoasCdUnico, Object[] sane) {
        complementarContato(pessoasCdUnico, 0L, sane[21]); // telefone
        complementarContato(pessoasCdUnico, 5L, sane[22]); // recado
        complementarContato(pessoasCdUnico, 1L, sane[23]); // celular
        complementarContato(pessoasCdUnico, 3L, sane[6]);  // email
        complementarContato(pessoasCdUnico, 4L, sane[24]); // pagina_web
    }
    private void complementarContato(
            Long pessoasCdUnico,
            Long tipoContato,
            Object valorContato
    ) {
        String contato = helper.normalizarContato(tipoContato, valorContato);

        if (contato == null || contato.isBlank()) {
            return;
        }

        Long existe = helper.countContato(
                pessoasCdUnico,
                tipoContato,
                contato
        );

        if (existe != null && existe > 0) {
            return;
        }

        manager.createNativeQuery("""
        insert into dbo_ccm_pessoas.contatos
        (
            id,
            pessoa_id,
            tipo_contato,
            contato
        )
        values
        (
            seq_contatos.nextval,
            :pessoaId,
            :tipoContato,
            :contato
        )
    """)
                .setParameter("pessoaId", pessoasCdUnico)
                .setParameter("tipoContato", tipoContato)
                .setParameter("contato", contato)
                .executeUpdate();
    }

    private Object[] buscarPessoaSane(Long pessoaId) {
        try {
            return (Object[]) manager.createNativeQuery("""
            select
                p.pessoa,
                p.nome,
                p.fisica_juridica,
                p.cgc_cpf,
                p.data_cadastro,
                p.tipo_pessoa,
                p.e_mail,
                p.pagina_web,
                p.telefone,
                p.recado,
                p.celular,
                p.pessoa_matriz,
                p.inscricao_estadual,
                p.fantasia,
                p.observacao,
                cast(null as date),
                cast(null as varchar2(1)),
                cast(null as varchar2(1)),
                cast(null as number),
                cast(null as number),
                cast(null as number),
                p.telefone,
                p.recado,
                p.celular,
                p.pagina_web,
                p.cidade,
                p.distrito,
                p.bairro,
                p.logradouro,
                p.numero,
                p.complemento,
                p.cep
            from dbo_ccm_pessoas.sane_pessoas p
            where p.pessoa = :pessoaId
        """)
                    .setParameter("pessoaId", pessoaId)
                    .getSingleResult();

        } catch (NoResultException e) {
            throw new IllegalArgumentException(
                    "Pessoa SANE não encontrada: " + pessoaId);
        }
    }
    private Long localizarPessoaJuridicaCadUnicoExistente(Object[] sane) {
        Long cnpj = helper.num(sane[3]);
        String nome = helper.str(sane[1]);

        if (cnpj == null || cnpj == 0L || nome == null || nome.isBlank()) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
            select pessoas_cd_unico
              from (
                    select
                        cup.pessoas_cd_unico,
                        case
                            when replace(fn_normaliza_texto(cup.nome), ' ', '') =
                                 replace(fn_normaliza_texto(:nome), ' ', '')
                            then 100

                            when replace(fn_normaliza_texto(cup.nome), ' ', '') like
                                 '%' || replace(fn_normaliza_texto(:nome), ' ', '') || '%'
                              or replace(fn_normaliza_texto(:nome), ' ', '') like
                                 '%' || replace(fn_normaliza_texto(cup.nome), ' ', '') || '%'
                            then 95

                            else utl_match.edit_distance_similarity(
                                    replace(fn_normaliza_texto(cup.nome), ' ', ''),
                                    replace(fn_normaliza_texto(:nome), ' ', '')
                                 )
                        end as similaridade
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                     where cup.cpf_cnpj = :cnpj
                       and cup.fisica_juridica = 'J'
                       and cup.pessoas_cd_unico is not null
                       and (
                            replace(fn_normaliza_texto(cup.nome), ' ', '') =
                            replace(fn_normaliza_texto(:nome), ' ', '')

                            or replace(fn_normaliza_texto(cup.nome), ' ', '') like
                               '%' || replace(fn_normaliza_texto(:nome), ' ', '') || '%'

                            or replace(fn_normaliza_texto(:nome), ' ', '') like
                               '%' || replace(fn_normaliza_texto(cup.nome), ' ', '') || '%'

                            or utl_match.edit_distance_similarity(
                                replace(fn_normaliza_texto(cup.nome), ' ', ''),
                                replace(fn_normaliza_texto(:nome), ' ', '')
                            ) >= 80
                       )
                     order by similaridade desc, cup.banco, cup.id
                   )
             where rownum = 1
        """)
                    .setParameter("cnpj", cnpj)
                    .setParameter("nome", nome)
                    .getSingleResult();

            return helper.num(result);

        } catch (Exception e) {
            return null;
        }
    }
    private void inserirVinculoCadUnicoPessoa(Long pessoasCdUnico, Object[] sane) {
        Long cadUnicoPessoaId = helper.getNextVal("DBO_CCM_PESSOAS.SEQ_CAD_UNICO_PESSOA");

        Long tipoPessoaId = helper.converterTipoPessoa(helper.num(sane[5]));
        String cnpj = helper.normalizarCpf(helper.num(sane[3]));

        manager.createNativeQuery("""
        insert into dbo_ccm_pessoas.cad_unico_pessoa
        (
            id,
            cd_origem,
            tipo_pessoa,
            nome,
            fisica_juridica,
            data_cadastro,
            cpf_cnpj,
            email,
            banco,
            pessoas_cd_unico,
            observacao
        )
        values
        (
            :id,
            :cdOrigem,
            :tipoPessoa,
            :nome,
            :fisicaJuridica,
            :dataCadastro,
            :cpfCnpj,
            :email,
            'SANE',
            :pessoasCdUnico,
            :observacao
        )
    """)
                .setParameter("id", cadUnicoPessoaId)
                .setParameter("cdOrigem", helper.num(sane[0]))
                .setParameter("tipoPessoa", tipoPessoaId)
                .setParameter("nome", helper.upper(helper.str(sane[1])))
                .setParameter("fisicaJuridica", helper.str(sane[2]))
                .setParameter("dataCadastro", sane[4])
                .setParameter("cpfCnpj", cnpj == null ? null : Long.valueOf(cnpj))
                .setParameter("email", helper.str(sane[6]))
                .setParameter("pessoasCdUnico", pessoasCdUnico)
                .setParameter("observacao", helper.str(sane[14]))
                .executeUpdate();
    }
    private void complementarDadosPj(Long pessoasCdUnico, Object[] sane) {

        manager.createNativeQuery("""
        update dbo_ccm_pessoas.dados_pj dpj
           set dpj.nome_fantasia =
                    case
                        when (dpj.nome_fantasia is null or trim(dpj.nome_fantasia) = '')
                             and :nomeFantasia is not null
                        then :nomeFantasia
                        else dpj.nome_fantasia
                    end,

               dpj.micro_empresa =
                    case
                        when dpj.micro_empresa is null
                        then 'N'
                        else dpj.micro_empresa
                    end
         where dpj.id = :pessoaId
    """)
                .setParameter("nomeFantasia", helper.upper(helper.str(sane[13])))
                .setParameter("pessoaId", pessoasCdUnico)
                .executeUpdate();
    }
}