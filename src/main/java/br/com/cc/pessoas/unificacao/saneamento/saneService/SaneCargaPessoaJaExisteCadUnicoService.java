package br.com.cc.pessoas.unificacao.saneamento.saneService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaneCargaPessoaJaExisteCadUnicoService {

    @PersistenceContext
    private EntityManager manager;

    private final SaneCargaPessoaHelperService helper;

    @Transactional
    public String processarJaExisteCadUnico(Long pessoaId) {

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
            return "Pessoa do saneamento já vinculada ao Cadastro Único.";
        }

        Object[] sane = buscarPessoaSane(pessoaId);

        Long pessoasCdUnico = localizarPessoaCadUnicoExistente(sane);

        if (pessoasCdUnico == null) {
            throw new IllegalArgumentException("Pessoa existente no Cadastro Único não localizada para o SANE: " + pessoaId);
        }

        inserirVinculoCadUnicoPessoa(pessoasCdUnico, sane);
        complementarDadosPf(pessoasCdUnico, sane);
        complementarContatos(pessoasCdUnico, sane);
        complementarDocumentos(pessoasCdUnico, sane);
        complementarEnderecos(pessoasCdUnico, sane);

        return "Pessoa SANE vinculada ao Cadastro Único existente. Código único: " + pessoasCdUnico + ".";
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
                    p.data_nascimento,
                    p.estado_civil,
                    p.sexo,
                    p.cidade_nascimento,
                    p.mae,
                    p.pai,
                    p.e_mail,
                    p.observacao,
                    p.tipo_pessoa,
                    p.tipo_documento,
                    p.numero_docto,
                    p.orgao_docto,
                    p.emissao_docto,
                    p.titulo_eleitoral,
                    p.zona,
                    p.secao,
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
            throw new IllegalArgumentException("Pessoa SANE não encontrada: " + pessoaId);
        }
    }

    private Long localizarPessoaCadUnicoExistente(Object[] sane) {
        Long cpf = helper.num(sane[3]);
        String nome = helper.str(sane[1]);

        if (cpf == null || cpf == 0L || nome == null || nome.isBlank()) {
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
                         where cup.cpf_cnpj = :cpf
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
                    .setParameter("cpf", cpf)
                    .setParameter("nome", nome)
                    .getSingleResult();

            return helper.num(result);

        } catch (Exception e) {
            return null;
        }
    }

    private void inserirVinculoCadUnicoPessoa(Long pessoasCdUnico, Object[] sane) {
        Long cadUnicoPessoaId = helper.getNextVal("DBO_CCM_PESSOAS.SEQ_CAD_UNICO_PESSOA");

        Long tipoPessoaId = helper.converterTipoPessoa(helper.num(sane[13]));
        String cpf = helper.normalizarCpf(helper.num(sane[3]));
        Long cidadeNascimentoCcm = helper.buscarCidadeNascimentoCcmSane(helper.num(sane[8]));

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
                data_nascimento,
                estado_civil,
                sexo,
                cidade_nascimento,
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
                :dataNascimento,
                :estadoCivil,
                :sexo,
                :cidadeNascimento,
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
                .setParameter("cpfCnpj", cpf == null ? null : Long.valueOf(cpf))
                .setParameter("dataNascimento", sane[5])
                .setParameter("estadoCivil", helper.str(sane[6]))
                .setParameter("sexo", helper.str(sane[7]))
                .setParameter("cidadeNascimento", cidadeNascimentoCcm)
                .setParameter("email", helper.str(sane[11]))
                .setParameter("pessoasCdUnico", pessoasCdUnico)
                .setParameter("observacao", helper.str(sane[12]))
                .executeUpdate();
    }

    private void complementarDadosPf(Long pessoasCdUnico, Object[] sane) {

        Integer estadoCivil = helper.mapearEstadoCivil(helper.str(sane[6]));
        Long cidadeNascimentoCcm = helper.buscarCidadeNascimentoCcmSane(helper.num(sane[8]));

        StringBuilder sql = new StringBuilder("""
            update dbo_ccm_pessoas.dados_pf dpf
               set dpf.sexo =
                        case
                            when dpf.sexo is null and :sexo is not null
                            then :sexo
                            else dpf.sexo
                        end,

                   dpf.mae =
                        case
                            when (dpf.mae is null or trim(dpf.mae) = '') and :mae is not null
                            then :mae
                            else dpf.mae
                        end,

                   dpf.pai =
                        case
                            when (dpf.pai is null or trim(dpf.pai) = '') and :pai is not null
                            then :pai
                            else dpf.pai
                        end
        """);

        if (cidadeNascimentoCcm != null) {
            sql.append("""
                ,
                   dpf.local_nascimento_id =
                        case
                            when dpf.local_nascimento_id is null
                            then :localNascimentoId
                            else dpf.local_nascimento_id
                        end
            """);
        }

        sql.append("""
             where dpf.id = :pessoaId
        """);

        var query = manager.createNativeQuery(sql.toString())
                .setParameter("sexo", helper.str(sane[7]))
                .setParameter("mae", helper.upper(helper.str(sane[9])))
                .setParameter("pai", helper.upper(helper.str(sane[10])))
                .setParameter("pessoaId", pessoasCdUnico);

        if (cidadeNascimentoCcm != null) {
            query.setParameter("localNascimentoId", cidadeNascimentoCcm);
        }

        query.executeUpdate();

        complementarEstadoCivil(pessoasCdUnico, estadoCivil);
        complementarDataNascimento(pessoasCdUnico, sane[5]);
    }

    private void complementarEstadoCivil(Long pessoasCdUnico, Integer estadoCivil) {

        if (estadoCivil == null) {
            return;
        }

        manager.createNativeQuery("""
        update dbo_ccm_pessoas.dados_pf dpf
           set dpf.estado_civil = :estadoCivil
         where dpf.id = :pessoaId
           and dpf.estado_civil is null
    """)
                .setParameter("estadoCivil", estadoCivil)
                .setParameter("pessoaId", pessoasCdUnico)
                .executeUpdate();
    }

    private void complementarDataNascimento(Long pessoasCdUnico, Object dataNascimento) {

        if (dataNascimento == null) {
            return;
        }

        manager.createNativeQuery("""
            update dbo_ccm_pessoas.dados_pf dpf
               set dpf.data_nascimento = :dataNascimento
             where dpf.id = :pessoaId
               and (
                    dpf.data_nascimento is null
                    or trunc(dpf.data_nascimento) = date '1900-01-01'
               )
        """)
                .setParameter("dataNascimento", dataNascimento)
                .setParameter("pessoaId", pessoasCdUnico)
                .executeUpdate();
    }

    private void complementarContatos(Long pessoasCdUnico, Object[] sane) {
        complementarContato(pessoasCdUnico, 0L, sane[21]);
        complementarContato(pessoasCdUnico, 5L, sane[22]);
        complementarContato(pessoasCdUnico, 1L, sane[23]);
        complementarContato(pessoasCdUnico, 3L, sane[11]);
        complementarContato(pessoasCdUnico, 4L, sane[24]);
    }

    private void complementarContato(Long pessoasCdUnico, Long tipoContato, Object valorContato) {

        String contato = helper.normalizarContato(tipoContato, valorContato);

        if (contato == null || contato.isBlank()) {
            return;
        }

        Long existe = helper.countContato(pessoasCdUnico, tipoContato, contato);

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

    private void complementarDocumentos(Long pessoasCdUnico, Object[] sane) {
        complementarDocumentoPrincipal(pessoasCdUnico, sane);
        complementarTituloEleitoral(pessoasCdUnico, sane);
    }

    private void complementarDocumentoPrincipal(Long pessoasCdUnico, Object[] sane) {

        Integer tipoDocumentoOrigem = helper.integer(sane[14]);
        Integer tipoDocumentoDestino = helper.mapearTipoDocumento(tipoDocumentoOrigem);

        String numeroDocumento = helper.str(sane[15]);

        if (tipoDocumentoDestino == null
                || numeroDocumento == null
                || numeroDocumento.isBlank()) {
            return;
        }

        Long existe = countDocumento(pessoasCdUnico, tipoDocumentoDestino, numeroDocumento);

        if (existe != null && existe > 0) {
            return;
        }

        manager.createNativeQuery("""
            insert into dbo_ccm_pessoas.documentos
            (
                id,
                pessoa_id,
                tipo_documento,
                numero_documento,
                orgao_expedidor,
                data_expedicao
            )
            values
            (
                seq_documentos.nextval,
                :pessoaId,
                :tipoDocumento,
                :numeroDocumento,
                :orgaoExpedidor,
                :dataExpedicao
            )
        """)
                .setParameter("pessoaId", pessoasCdUnico)
                .setParameter("tipoDocumento", tipoDocumentoDestino)
                .setParameter("numeroDocumento", numeroDocumento)
                .setParameter("orgaoExpedidor", helper.str(sane[16]))
                .setParameter("dataExpedicao", sane[17])
                .executeUpdate();
    }

    private void complementarTituloEleitoral(Long pessoasCdUnico, Object[] sane) {

        Long tituloEleitoral = helper.num(sane[18]);

        if (tituloEleitoral == null || tituloEleitoral == 0L) {
            return;
        }

        Long existe = countDocumento(pessoasCdUnico, 6, String.valueOf(tituloEleitoral));

        if (existe != null && existe > 0) {
            return;
        }

        manager.createNativeQuery("""
            insert into dbo_ccm_pessoas.documentos
            (
                id,
                pessoa_id,
                tipo_documento,
                numero_documento,
                zona,
                secao
            )
            values
            (
                seq_documentos.nextval,
                :pessoaId,
                6,
                :numeroDocumento,
                :zona,
                :secao
            )
        """)
                .setParameter("pessoaId", pessoasCdUnico)
                .setParameter("numeroDocumento", tituloEleitoral)
                .setParameter("zona", helper.num(sane[19]))
                .setParameter("secao", helper.num(sane[20]))
                .executeUpdate();
    }

    private Long countDocumento(Long pessoaId, Integer tipoDocumento, String numeroDocumento) {
        return ((Number) manager.createNativeQuery("""
            select count(1)
              from dbo_ccm_pessoas.documentos d
             where d.pessoa_id = :pessoaId
               and d.tipo_documento = :tipoDocumento
               and trim(d.numero_documento) = trim(:numeroDocumento)
        """)
                .setParameter("pessoaId", pessoaId)
                .setParameter("tipoDocumento", tipoDocumento)
                .setParameter("numeroDocumento", numeroDocumento)
                .getSingleResult()).longValue();
    }

    private void complementarEnderecos(Long pessoasCdUnico, Object[] sane) {
        helper.complementarEnderecosSane(
                pessoasCdUnico,
                java.util.Collections.singletonList(sane)
        );
    }
}