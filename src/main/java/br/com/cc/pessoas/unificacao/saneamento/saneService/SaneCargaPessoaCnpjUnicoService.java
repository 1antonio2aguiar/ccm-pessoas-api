package br.com.cc.pessoas.unificacao.saneamento.saneService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaneCargaPessoaCnpjUnicoService {

    @PersistenceContext
    private EntityManager manager;

    private final SaneCargaPessoaHelperService helper;

    @Transactional
    public String processarCnpjUnico(Long pessoaId) {

        if (pessoaId == null) {
            throw new IllegalArgumentException("Código da pessoa do saneamento não informado.");
        }

        Object[] sane = buscarPessoaSane(pessoaId);

        if (!"J".equalsIgnoreCase(helper.str(sane[2]))) {
            throw new IllegalArgumentException("Pessoa SANE não é pessoa jurídica: " + pessoaId);
        }

        Long cnpj = helper.num(sane[3]);

        if (cnpj == null || cnpj == 0L) {
            throw new IllegalArgumentException("CNPJ não informado para pessoa SANE: " + pessoaId);
        }

        Long qtdMesmoCnpjSane = ((Number) manager.createNativeQuery("""
            select count(1)
              from dbo_ccm_pessoas.sane_pessoas p
             where p.fisica_juridica = 'J'
               and p.cgc_cpf = :cnpj
        """)
                .setParameter("cnpj", cnpj)
                .getSingleResult()).longValue();

        if (qtdMesmoCnpjSane > 1) {
            throw new IllegalArgumentException("CNPJ duplicado no SANE. Use a rotina de CNPJ duplicado.");
        }

        Long existeVinculoSane = helper.count("""
            select count(1)
              from dbo_ccm_pessoas.cad_unico_pessoa cup
             where cup.cd_origem = :pessoaId
               and cup.banco = 'SANE'
        """, "pessoaId", pessoaId);

        if (existeVinculoSane > 0) {
            throw new IllegalArgumentException("Pessoa SANE já vinculada ao Cadastro Único.");
        }

        Long novoPessoaId = inserirPessoa(sane);
        inserirDadosPj(novoPessoaId, sane);
        inserirCadUnicoPessoa(novoPessoaId, sane);
        inserirContatos(novoPessoaId, sane);
        inserirEnderecos(novoPessoaId, sane);

        return "Pessoa jurídica SANE migrada com sucesso. Código único: " + novoPessoaId + ".";
    }
    private void inserirEnderecos(Long novoPessoaId, Object[] sane) {
        helper.inserirEnderecosUnicosSane(
                novoPessoaId,
                java.util.Collections.singletonList(sane)
        );
    }
    private void inserirContatos(Long novoPessoaId, Object[] sane) {

        inserirContato(novoPessoaId, 0L, sane[8]);  // telefone
        inserirContato(novoPessoaId, 5L, sane[9]);  // recado
        inserirContato(novoPessoaId, 1L, sane[10]); // celular
        inserirContato(novoPessoaId, 3L, sane[6]);  // email
        inserirContato(novoPessoaId, 4L, sane[7]);  // pagina web
    }
    private void inserirContato(
            Long pessoaId,
            Long tipoContato,
            Object valorContato) {

        String contato =
                helper.normalizarContato(tipoContato, valorContato);

        if (contato == null || contato.isBlank()) {
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
                .setParameter("pessoaId", pessoaId)
                .setParameter("tipoContato", tipoContato)
                .setParameter("contato", contato)
                .executeUpdate();
    }

    private Object[] buscarPessoaSane(Long pessoaId) {
        try {
            return (Object[]) manager.createNativeQuery("""
            select
                p.pessoa,              -- 0
                p.nome,                -- 1
                p.fisica_juridica,     -- 2
                p.cgc_cpf,             -- 3
                p.data_cadastro,       -- 4
                p.tipo_pessoa,         -- 5
                p.e_mail,              -- 6
                p.pagina_web,          -- 7
                p.telefone,            -- 8
                p.recado,              -- 9
                p.celular,             -- 10
                p.pessoa_matriz,       -- 11
                p.inscricao_estadual,  -- 12
                p.fantasia,            -- 13
                p.observacao,          -- 14
                cast(null as date),    -- 15 reservado
                cast(null as varchar2(1)), -- 16 reservado
                cast(null as varchar2(1)), -- 17 reservado
                cast(null as number),  -- 18 reservado
                cast(null as number),  -- 19 reservado
                cast(null as number),  -- 20 reservado
                p.telefone,            -- 21
                p.recado,              -- 22
                p.celular,             -- 23
                p.pagina_web,          -- 24
                p.cidade,              -- 25
                p.distrito,            -- 26
                p.bairro,              -- 27
                p.logradouro,          -- 28
                p.numero,              -- 29
                p.complemento,         -- 30
                p.cep                  -- 31
              from dbo_ccm_pessoas.sane_pessoas p
             where p.pessoa = :pessoaId
        """)
                    .setParameter("pessoaId", pessoaId)
                    .getSingleResult();

        } catch (NoResultException e) {
            throw new IllegalArgumentException("Pessoa SANE não encontrada: " + pessoaId);
        }
    }
    private Long inserirPessoa(Object[] sane) {
        Long novoPessoaId = helper.getNextVal("DBO_CCM_PESSOAS.SEQ_PESSOAS");

        Long tipoPessoaId = helper.converterTipoPessoa(helper.num(sane[5]));

        manager.createNativeQuery("""
        insert into dbo_ccm_pessoas.pessoas
        (
            id,
            tipo_pessoa_id,
            nome,
            data_cadastro,
            fisica_juridica,
            observacao,
            situacao_id
        )
        values
        (
            :id,
            :tipoPessoaId,
            :nome,
            :dataCadastro,
            :fisicaJuridica,
            :observacao,
            1
        )
    """)
        .setParameter("id", novoPessoaId)
        .setParameter("tipoPessoaId", tipoPessoaId)
        .setParameter("nome", helper.upper(helper.str(sane[1])))
        .setParameter("dataCadastro", sane[4])
        .setParameter("fisicaJuridica", helper.str(sane[2]))
        .setParameter("observacao", helper.str(sane[14]))
        .executeUpdate();

        return novoPessoaId;
    }
    private void inserirDadosPj(Long novoPessoaId, Object[] sane) {
        String cnpj = helper.normalizarCpf(helper.num(sane[3]));

        manager.createNativeQuery("""
        insert into dbo_ccm_pessoas.dados_pj
        (
            id,
            cnpj,
            nome_fantasia,
            objeto_social,
            micro_empresa,
            tipo_empresa
        )
        values
        (
            :id,
            :cnpj,
            :nomeFantasia,
            :objetoSocial,
            :microEmpresa,
            :tipoEmpresa
        )
    """)
                .setParameter("id", novoPessoaId)
                .setParameter("cnpj", cnpj == null ? null : Long.valueOf(cnpj))
                .setParameter("nomeFantasia", helper.upper(helper.str(sane[13])))
                .setParameter("objetoSocial", null)
                .setParameter("microEmpresa", "N")
                .setParameter("tipoEmpresa", null)
                .executeUpdate();
    }
    private void inserirCadUnicoPessoa(Long novoPessoaId, Object[] sane) {

        Long id = helper.getNextVal("DBO_CCM_PESSOAS.SEQ_CAD_UNICO_PESSOA");

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
            banco,
            pessoas_cd_unico,
            observacao,
            email
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
            'SANE',
            :pessoasCdUnico,
            :observacao,
            :email
        )
    """)
                .setParameter("id", id)
                .setParameter("cdOrigem", helper.num(sane[0]))
                .setParameter("tipoPessoa", tipoPessoaId)
                .setParameter("nome", helper.upper(helper.str(sane[1])))
                .setParameter("fisicaJuridica", helper.str(sane[2]))
                .setParameter("dataCadastro", sane[4])
                .setParameter("cpfCnpj", cnpj == null ? null : Long.valueOf(cnpj))
                .setParameter("pessoasCdUnico", novoPessoaId)
                .setParameter("observacao", helper.str(sane[14]))
                .setParameter("email", helper.str(sane[6]))
                .executeUpdate();
    }
}