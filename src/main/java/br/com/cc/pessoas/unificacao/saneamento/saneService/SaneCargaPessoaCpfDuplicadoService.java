package br.com.cc.pessoas.unificacao.saneamento.saneService;

import br.com.cc.pessoas.unificacao.controle.ctrlService.ControleMigracaoPessoaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SaneCargaPessoaCpfDuplicadoService {

    @PersistenceContext
    private EntityManager manager;

    private final SaneCargaPessoaHelperService helper;
    private final ControleMigracaoPessoaService controleMigracaoPessoaService;
    @Transactional
    public String processarPessoaDuplicada(
            Long pessoaId) {

        return processarGrupoCpfDuplicado(
                pessoaId,
                false
        );
    }

    @Transactional
    public String processarPessoaDuplicadaJaExiste(
            Long pessoaId) {

        return processarGrupoCpfDuplicado(
                pessoaId,
                true
        );
    }
    /*
     * Registra como "não migrar" todos os integrantes
     * do grupo de CPF duplicado do Saneamento.
     */
    @Transactional
    public void registrarGrupoNaoMigrar(
            Long pessoaId) {

        if (pessoaId == null
                || pessoaId <= 0L) {

            throw new IllegalArgumentException(
                    "Código da pessoa do saneamento inválido."
            );
        }

        Object[] saneBase =
                buscarPessoaSane(
                        pessoaId
                );

        String fisicaJuridica =
                helper.str(
                        saneBase[2]
                );

        Long cpfNumerico =
                helper.num(
                        saneBase[3]
                );

        if (!"F".equalsIgnoreCase(
                fisicaJuridica
        )) {

            throw new IllegalArgumentException(
                    "Pessoa SANE não é pessoa física."
            );
        }

        if (cpfNumerico == null
                || cpfNumerico == 0L) {

            throw new IllegalArgumentException(
                    "Pessoa SANE não possui CPF válido."
            );
        }

        String nomeBase =
                helper.str(
                        saneBase[1]
                );

        List<Object[]> grupo =
                buscarGrupoCpfDuplicado(
                        cpfNumerico,
                        nomeBase
                );

        if (grupo == null
                || grupo.size() <= 1) {

            throw new IllegalArgumentException(
                    "CPF não é duplicado no saneamento."
            );
        }

        List<Long> codigosOrigem =
                grupo.stream()
                        .map(
                                sane ->
                                        helper.num(
                                                sane[0]
                                        )
                        )
                        .toList();

        controleMigracaoPessoaService
                .registrarGrupoNaoMigrar(
                        "SANE",
                        codigosOrigem
                );
    }

    private String processarGrupoCpfDuplicado(
            Long pessoaId,
            boolean vincularPessoaExistente) {

        if (pessoaId == null) {
            throw new IllegalArgumentException(
                    "Código da pessoa do saneamento não informado."
            );
        }

        Object[] saneBase =
                buscarPessoaSane(
                        pessoaId
                );

        String fisicaJuridica =
                helper.str(
                        saneBase[2]
                );

        Long cpfNumerico =
                helper.num(
                        saneBase[3]
                );

        if (!"F".equalsIgnoreCase(fisicaJuridica)) {
            throw new IllegalArgumentException(
                    "Pessoa SANE não é pessoa física."
            );
        }

        if (cpfNumerico == null || cpfNumerico == 0L) {
            throw new IllegalArgumentException(
                    "Pessoa SANE não possui CPF válido."
            );
        }

        String nomeBase =
                helper.str(
                        saneBase[1]
                );

        List<Object[]> grupo =
            buscarGrupoCpfDuplicado(
                    cpfNumerico,
                    nomeBase
            );

        if (grupo.size() <= 1) {
            throw new IllegalArgumentException(
                    "CPF não é duplicado no saneamento. "
                            + "Use a rotina de CPF único."
            );
        }

        validarGrupoNaoVinculado(
                grupo
        );

        String cpf =
                helper.normalizarCpf(
                        cpfNumerico
                );

        Long pessoaExistenteCadUnico =
                buscarPessoaExistenteNoCadUnico(
                        cpf
                );

        /*
         * O CPF já existe no Cadastro Único.
         */
        if (pessoaExistenteCadUnico != null) {

            /*
             * O processamento normal não pode vincular
             * automaticamente.
             *
             * Essa vinculação somente será permitida quando:
             *
             * 1. CPF e nome forem iguais; ou
             * 2. o usuário confirmar o modal.
             */
            if (!vincularPessoaExistente) {
                throw new IllegalArgumentException(
                        "O CPF já existe no Cadastro Único. "
                                + "A vinculação precisa ser confirmada."
                );
            }

            vincularGrupoPessoaExistente(
                    pessoaExistenteCadUnico,
                    grupo,
                    cpf
            );

            return "CPF duplicado SANE vinculado à pessoa existente "
                    + "no Cadastro Único. ID: "
                    + pessoaExistenteCadUnico
                    + ". Total vinculado: "
                    + grupo.size()
                    + ".";
        }

        /*
         * O frontend solicitou uma vinculação com pessoa
         * existente, mas o CPF não foi encontrado.
         */
        if (vincularPessoaExistente) {
            throw new IllegalArgumentException(
                    "Não foi encontrada uma pessoa no Cadastro Único "
                            + "com o CPF informado."
            );
        }

        /*
         * CPF ainda não existe:
         * cria uma nova pessoa para todo o grupo.
         */
        Long novoPessoaId =
                helper.getNextVal(
                        "DBO_CCM_PESSOAS.SEQ_PESSOAS"
                );

        Long tipoPessoaId =
                helper.converterTipoPessoa(
                        helper.num(
                                saneBase[13]
                        )
                );

        Integer estadoCivil =
                helper.mapearEstadoCivil(
                        helper.str(
                                saneBase[6]
                        )
                );

        Long cidadeNascimentoCcm =
                helper.buscarCidadeNascimentoCcmSane(
                        helper.num(
                                saneBase[8]
                        )
                );

        inserirPessoa(
                novoPessoaId,
                saneBase,
                tipoPessoaId
        );

        inserirDadosPf(
                novoPessoaId,
                saneBase,
                cpf,
                estadoCivil,
                cidadeNascimentoCcm
        );

        for (Object[] sane : grupo) {

            Long tipoPessoaOrigem =
                    helper.converterTipoPessoa(
                            helper.num(
                                    sane[13]
                            )
                    );

            Long cidadeNascimentoOrigem =
                    helper.buscarCidadeNascimentoCcmSane(
                            helper.num(
                                    sane[8]
                            )
                    );

            inserirCadUnicoPessoa(
                    novoPessoaId,
                    sane,
                    tipoPessoaOrigem,
                    cpf,
                    cidadeNascimentoOrigem
            );
        }

        inserirDocumentosPf(
                novoPessoaId,
                grupo
        );

        inserirContatos(
                novoPessoaId,
                grupo
        );

        helper.inserirEnderecosUnicosSane(
                novoPessoaId,
                grupo
        );

        return "Pessoas SANE com CPF duplicado migradas com sucesso. "
                + "Novo ID: "
                + novoPessoaId
                + ". Total vinculado: "
                + grupo.size()
                + ".";
    }
    private void vincularGrupoPessoaExistente(
            Long pessoaExistenteCadUnico,
            List<Object[]> grupo,
            String cpf) {

        for (Object[] sane : grupo) {

            Long tipoPessoaOrigem =
                    helper.converterTipoPessoa(
                            helper.num(
                                    sane[13]
                            )
                    );

            Long cidadeNascimentoOrigem =
                    helper.buscarCidadeNascimentoCcmSane(
                            helper.num(
                                    sane[8]
                            )
                    );

            inserirCadUnicoPessoa(
                    pessoaExistenteCadUnico,
                    sane,
                    tipoPessoaOrigem,
                    cpf,
                    cidadeNascimentoOrigem
            );
        }

        inserirDocumentosPf(
                pessoaExistenteCadUnico,
                grupo
        );

        inserirContatos(
                pessoaExistenteCadUnico,
                grupo
        );

        helper.inserirEnderecosUnicosSane(
                pessoaExistenteCadUnico,
                grupo
        );
    }

    private Object[] buscarPessoaSane(Long pessoaId) {
        try {
            return (Object[]) manager.createNativeQuery(sqlPessoaSane() + """
                 where p.pessoa = :pessoaId
            """)
                    .setParameter("pessoaId", pessoaId)
                    .getSingleResult();

        } catch (NoResultException e) {
            throw new IllegalArgumentException("Pessoa SANE não encontrada: " + pessoaId);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> buscarGrupoCpfDuplicado(
            Long cpfNumerico,
            String nomeBase) {

        return manager.createNativeQuery(
                        sqlPessoaSane() + """
                 where p.fisica_juridica = 'F'
                   and p.cgc_cpf = :cpf
                   and replace(
                           fn_normaliza_texto(p.nome),
                           ' ',
                           ''
                       ) =
                       replace(
                           fn_normaliza_texto(:nome),
                           ' ',
                           ''
                       )
                 order by p.pessoa
            """
                )
                .setParameter(
                        "cpf",
                        cpfNumerico
                )
                .setParameter(
                        "nome",
                        nomeBase
                )
                .getResultList();
    }

    private void validarGrupoNaoVinculado(List<Object[]> grupo) {
        for (Object[] sane : grupo) {
            Long pessoaOrigem = helper.num(sane[0]);

            Long existeVinculo = helper.count("""
                select count(1)
                  from dbo_ccm_pessoas.cad_unico_pessoa cup
                 where cup.cd_origem = :pessoaId
                   and cup.banco = 'SANE'
            """, "pessoaId", pessoaOrigem);

            if (existeVinculo > 0) {
                throw new IllegalArgumentException(
                        "Pessoa do saneamento já vinculada ao Cadastro Único: " + pessoaOrigem
                );
            }
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

    private void inserirDocumentosPf(Long novoPessoaId, List<Object[]> grupo) {
        Set<String> documentosJaInseridos = new HashSet<>();

        for (Object[] sane : grupo) {
            Integer tipoDocumentoDestino = helper.mapearTipoDocumento(helper.integer(sane[14]));
            String numeroDocumento = helper.str(sane[15]);

            if (tipoDocumentoDestino != null && numeroDocumento != null && !numeroDocumento.isBlank()) {
                String chaveDocumento = tipoDocumentoDestino + "|" + numeroDocumento.trim().toUpperCase();

                if (!documentosJaInseridos.contains(chaveDocumento)) {
                    documentosJaInseridos.add(chaveDocumento);

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
                            .setParameter("numeroDocumento", numeroDocumento.trim())
                            .setParameter("orgaoExpedidor", helper.str(sane[16]))
                            .setParameter("dataExpedicao", sane[17])
                            .executeUpdate();
                }
            }

            Long tituloEleitoral = helper.num(sane[18]);

            if (tituloEleitoral != null && tituloEleitoral != 0L) {
                String chaveTitulo = "6|" + tituloEleitoral;

                if (!documentosJaInseridos.contains(chaveTitulo)) {
                    documentosJaInseridos.add(chaveTitulo);

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
        }
    }

    private void inserirContatos(Long novoPessoaId, List<Object[]> grupo) {
        Set<String> contatosJaInseridos = new HashSet<>();

        for (Object[] sane : grupo) {
            inserirContato(novoPessoaId, 0L, sane[21], contatosJaInseridos);
            inserirContato(novoPessoaId, 5L, sane[22], contatosJaInseridos);
            inserirContato(novoPessoaId, 1L, sane[23], contatosJaInseridos);
            inserirContato(novoPessoaId, 3L, sane[11], contatosJaInseridos);
            inserirContato(novoPessoaId, 4L, sane[24], contatosJaInseridos);
        }
    }

    private void inserirContato(
            Long novoPessoaId,
            Long tipoContato,
            Object contato,
            Set<String> contatosJaInseridos
    ) {
        String contatoNormalizado = helper.normalizarContato(tipoContato, contato);

        if (contatoNormalizado == null || contatoNormalizado.isBlank()) {
            return;
        }

        String chaveContato = tipoContato + "|" + contatoNormalizado.trim().toUpperCase();

        if (contatosJaInseridos.contains(chaveContato)) {
            return;
        }

        contatosJaInseridos.add(chaveContato);

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

    private String sqlPessoaSane() {
        return """
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
        """;
    }

    private Long buscarPessoaExistenteNoCadUnico(
            String cpf) {

        if (cpf == null || cpf.isBlank()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Number> ids =
                manager.createNativeQuery("""
                select id
                  from (
                        select dpf.id
                          from dbo_ccm_pessoas.dados_pf dpf
                         where trim(dpf.cpf) = :cpf
                         order by dpf.id
                       )
                 where rownum = 1
            """)
                        .setParameter(
                                "cpf",
                                cpf
                        )
                        .getResultList();

        if (ids == null || ids.isEmpty()) {
            return null;
        }

        return ids.get(0).longValue();
    }
}