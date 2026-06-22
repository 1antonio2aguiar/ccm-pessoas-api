package br.com.cc.pessoas.unificacao.saneamento.saneService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SaneCargaPessoaCpfUnicoService {

    @PersistenceContext
    private EntityManager manager;

    private final SaneCargaPessoaHelperService helper;

    @Transactional
    public String processarPessoaUnica(Long pessoaId) {

        if (pessoaId == null) {
            throw new IllegalArgumentException("Código da pessoa do saneamento não informado.");
        }

        Long existeVinculo = helper.count("""
            select count(1)
              from dbo_ccm_pessoas.cad_unico_pessoa cup
             where cup.cd_origem = :pessoaId
               and cup.banco = 'SANE'
        """, "pessoaId", pessoaId);

        if (existeVinculo > 0) {
            return "Pessoa do saneamento já vinculada ao Cadastro Único.";
        }

        Object[] sane = buscarPessoaSane(pessoaId);

        String fisicaJuridica = helper.str(sane[2]);
        Long cpfNumerico = helper.num(sane[3]);

        if (!"F".equalsIgnoreCase(fisicaJuridica)) {
            throw new IllegalArgumentException("Pessoa SANE não é pessoa física.");
        }

        if (cpfNumerico == null || cpfNumerico == 0L) {
            throw new IllegalArgumentException("Pessoa SANE não possui CPF válido.");
        }

        Long qtdMesmoCpf = helper.count("""
            select count(1)
              from dbo_ccm_pessoas.sane_pessoas p
             where p.fisica_juridica = 'F'
               and p.cgc_cpf = :cpf
        """, "cpf", cpfNumerico);

        if (qtdMesmoCpf > 1) {
            throw new IllegalArgumentException("CPF não é único no saneamento.");
        }

        Long novoPessoaId = helper.getNextVal("DBO_CCM_PESSOAS.SEQ_PESSOAS");

        String cpf = helper.normalizarCpf(cpfNumerico);
        Long tipoPessoaId = helper.converterTipoPessoa(helper.num(sane[13]));
        Integer estadoCivil = helper.mapearEstadoCivil(helper.str(sane[6]));
        Long cidadeNascimentoCcm = helper.buscarCidadeNascimentoCcmSane(helper.num(sane[8]));

        inserirPessoa(novoPessoaId, sane, tipoPessoaId);
        inserirDadosPf(novoPessoaId, sane, cpf, estadoCivil, cidadeNascimentoCcm);
        inserirCadUnicoPessoa(novoPessoaId, sane, tipoPessoaId, cpf, cidadeNascimentoCcm);
        inserirDocumentos(novoPessoaId, sane);
        inserirContatos(novoPessoaId, sane);
        helper.inserirEnderecosUnicosSane(novoPessoaId, Collections.singletonList(sane));

        return "Pessoa SANE migrada com sucesso. Novo ID: " + novoPessoaId + ".";
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
                    p.data_nascimento,     -- 5
                    p.estado_civil,        -- 6
                    p.sexo,                -- 7
                    p.cidade_nascimento,   -- 8
                    p.mae,                 -- 9
                    p.pai,                 -- 10
                    p.e_mail,              -- 11
                    p.observacao,          -- 12
                    p.tipo_pessoa,         -- 13
                    p.tipo_documento,      -- 14
                    p.numero_docto,        -- 15
                    p.orgao_docto,         -- 16
                    p.emissao_docto,       -- 17
                    p.titulo_eleitoral,    -- 18
                    p.zona,                -- 19
                    p.secao,               -- 20
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

    private void inserirPessoa(Long novoPessoaId, Object[] sane, Long tipoPessoaId) {
        manager.createNativeQuery("""
            insert into dbo_ccm_pessoas.pessoas (
                id,
                tipo_pessoa_id,
                nome,
                fisica_juridica,
                data_cadastro,
                observacao,
                situacao_id
            ) values (
                :id,
                :tipoPessoaId,
                :nome,
                'F',
                sysdate,
                :observacao,
                1
            )
        """)
                .setParameter("id", novoPessoaId)
                .setParameter("tipoPessoaId", tipoPessoaId)
                .setParameter("nome", helper.upper(helper.str(sane[1])))
                .setParameter("observacao", helper.str(sane[12]))
                .executeUpdate();
    }

    private void inserirDadosPf(
            Long novoPessoaId,
            Object[] sane,
            String cpf,
            Integer estadoCivil,
            Long cidadeNascimentoCcm
    ) {
        manager.createNativeQuery("""
            insert into dbo_ccm_pessoas.dados_pf (
                id,
                cpf,
                sexo,
                estado_civil,
                local_nascimento_id,
                mae,
                pai,
                data_nascimento
            ) values (
                :id,
                :cpf,
                :sexo,
                :estadoCivil,
                :localNascimentoId,
                :mae,
                :pai,
                :dataNascimento
            )
        """)
                .setParameter("id", novoPessoaId)
                .setParameter("cpf", cpf)
                .setParameter("sexo", helper.str(sane[7]))
                .setParameter("estadoCivil", estadoCivil)
                .setParameter("localNascimentoId", cidadeNascimentoCcm)
                .setParameter("mae", helper.upper(helper.str(sane[9])))
                .setParameter("pai", helper.upper(helper.str(sane[10])))
                .setParameter("dataNascimento", sane[5])
                .executeUpdate();
    }

    private void inserirCadUnicoPessoa(
            Long novoPessoaId,
            Object[] sane,
            Long tipoPessoaId,
            String cpf,
            Long cidadeNascimentoCcm
    ) {
        Long cadUnicoPessoaId = helper.getNextVal("DBO_CCM_PESSOAS.SEQ_CAD_UNICO_PESSOA");

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
                'F',
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
                .setParameter("dataCadastro", sane[4])
                .setParameter("cpfCnpj", cpf == null ? null : Long.valueOf(cpf))
                .setParameter("dataNascimento", sane[5])
                .setParameter("estadoCivil", helper.str(sane[6]))
                .setParameter("sexo", helper.str(sane[7]))
                .setParameter("cidadeNascimento", cidadeNascimentoCcm)
                .setParameter("email", helper.str(sane[11]))
                .setParameter("pessoasCdUnico", novoPessoaId)
                .setParameter("observacao", helper.str(sane[12]))
                .executeUpdate();
    }

    private void inserirDocumentos(Long novoPessoaId, Object[] sane) {
        Integer tipoDocumentoDestino = helper.mapearTipoDocumento(helper.integer(sane[14]));
        String numeroDocumento = helper.str(sane[15]);

        if (tipoDocumentoDestino != null && numeroDocumento != null && !numeroDocumento.isBlank()) {
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
                    .setParameter("pessoaId", novoPessoaId)
                    .setParameter("tipoDocumento", tipoDocumentoDestino)
                    .setParameter("numeroDocumento", numeroDocumento)
                    .setParameter("orgaoExpedidor", helper.str(sane[16]))
                    .setParameter("dataExpedicao", sane[17])
                    .executeUpdate();
        }

        Long tituloEleitoral = helper.num(sane[18]);

        if (tituloEleitoral != null && tituloEleitoral != 0L) {
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
                    .setParameter("pessoaId", novoPessoaId)
                    .setParameter("numeroDocumento", tituloEleitoral)
                    .setParameter("zona", helper.num(sane[19]))
                    .setParameter("secao", helper.num(sane[20]))
                    .executeUpdate();
        }
    }

    private void inserirContatos(Long novoPessoaId, Object[] sane) {
        inserirContato(novoPessoaId, 0L, sane[21]);
        inserirContato(novoPessoaId, 5L, sane[22]);
        inserirContato(novoPessoaId, 1L, sane[23]);
        inserirContato(novoPessoaId, 3L, sane[11]);
        inserirContato(novoPessoaId, 4L, sane[24]);
    }

    private void inserirContato(Long novoPessoaId, Long tipoContato, Object contato) {
        String contatoNormalizado = helper.normalizarContato(tipoContato, contato);

        if (contatoNormalizado == null || contatoNormalizado.isBlank()) {
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
                .setParameter("pessoaId", novoPessoaId)
                .setParameter("tipoContato", tipoContato)
                .setParameter("contato", contatoNormalizado)
                .executeUpdate();
    }
}