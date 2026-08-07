package br.com.cc.pessoas.unificacao.pessoa.pesService;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPessoa;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PesPessoaJaExisteCadUnicoService
        extends PesPessoaBaseService {

    @Autowired
    private ProcessarEmpresasVinculadas
            processarEmpresasVinculadas;

    @Transactional
    public void processarPessoaUnica(
            Long pessoaId) {

        if (pessoaId == null) {
            throw new IllegalArgumentException(
                    "Código da pessoa não informado."
            );
        }

        PesPessoa pessoa =
                pesPessoaRepository
                        .findById(pessoaId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Pessoa não encontrada: "
                                                + pessoaId
                                )
                        );

        if (!"F".equalsIgnoreCase(
                pessoa.getFisicaJuridica()
        )) {
            throw new RuntimeException(
                    "O registro "
                            + pessoaId
                            + " não é uma pessoa física."
            );
        }

        String cpf =
                normalizarCpf(
                        pessoa.getCnpjCpf()
                );

        if (cpf == null || cpf.isBlank()) {
            throw new RuntimeException(
                    "CPF não informado para a pessoa "
                            + pessoaId
                            + "."
            );
        }

        Long pessoaCadastroUnicoId =
                buscarPessoaExistentePorCpf(
                        cpf
                );

        if (pessoaCadastroUnicoId == null) {
            throw new RuntimeException(
                    "O CPF "
                            + cpf
                            + " não foi localizado no Cadastro Único."
            );
        }

        if (jaExisteVinculo(
                pessoa.getPessoa()
        )) {
            return;
        }

        Long tipoPessoa =
            getTipoPessoa(
                pessoa
        );

        Integer estadoCivil =
        mapearEstadoCivil(
                pessoa.getEstadoCivil()
        );

        Long cidadeNascimentoCcm =
        buscarCidadeNascimentoCcm(
                pessoa.getCidadeNascimento()
        );

        /*
         * Insere somente a origem em CAD_UNICO_PESSOA.
         *
         * Não cria outro registro em PESSOAS nem em DADOS_PF,
         * pois eles já existem para esse CPF.
         */
        inserirCadUnicoPessoaFisica(
                pessoaCadastroUnicoId,
                pessoa,
                tipoPessoa,
                cpf,
                cidadeNascimentoCcm
        );

        complementarPessoa(
                pessoaCadastroUnicoId,
                pessoa,
                tipoPessoa
        );

        complementarDadosPf(
                pessoaCadastroUnicoId,
                pessoa,
                estadoCivil,
                cidadeNascimentoCcm
        );
        inserirEnderecoSeNaoExistir(
                pessoaCadastroUnicoId,
                pessoa
        );
        inserirDocumentosSeNaoExistirem(
                pessoaCadastroUnicoId,
                pessoa
        );
        inserirContatosSeNaoExistirem(
                pessoaCadastroUnicoId,
                pessoa
        );

        /*
         * As complementações de PESSOAS, DADOS_PF,
         * endereços, documentos e contatos serão chamadas
         * neste ponto nos próximos passos.
         */

        processarEmpresasVinculadas.processar(
                pessoa,
                pessoaCadastroUnicoId
        );
    }

    protected Long buscarPessoaExistentePorCpf(
            String cpf) {

        if (cpf == null || cpf.isBlank()) {
            return null;
        }

        List<?> resultados =
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

        if (resultados.isEmpty()) {
            return null;
        }

        return ((Number) resultados.get(0))
                .longValue();
    }

    protected boolean jaExisteVinculo(
            Long pessoaOrigemId) {

        if (pessoaOrigemId == null) {
            return false;
        }

        Number quantidade =
                (Number) manager.createNativeQuery("""
                    select count(1)
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                     where cup.cd_origem = :pessoaOrigemId
                       and cup.banco = 'PESSOAS'
                """)
                        .setParameter(
                                "pessoaOrigemId",
                                pessoaOrigemId
                        )
                        .getSingleResult();

        return quantidade != null
                && quantidade.longValue() > 0;
    }
    protected void complementarPessoa(
            Long pessoaCadastroUnicoId,
            PesPessoa pessoa,
            Long tipoPessoa) {

        manager.createNativeQuery("""
        update dbo_ccm_pessoas.pessoas p
           set p.tipo_pessoa_id =
                   case
                       when p.tipo_pessoa_id is null
                       then :tipoPessoa
                       else p.tipo_pessoa_id
                   end,

               p.data_cadastro =
                   case
                       when p.data_cadastro is null
                       then :dataCadastro
                       else p.data_cadastro
                   end,

               p.observacao =
                   case
                       when (p.observacao is null
                             or trim(p.observacao) = '')
                       then :observacao
                       else p.observacao
                   end

         where p.id = :pessoaId
    """)
                .setParameter(
                        "tipoPessoa",
                        tipoPessoa
                )
                .setParameter(
                        "dataCadastro",
                        pessoa.getDataCadastro()
                )
                .setParameter(
                        "observacao",
                        pessoa.getObservacao()
                )
                .setParameter(
                        "pessoaId",
                        pessoaCadastroUnicoId
                )
                .executeUpdate();
    }
    protected void complementarDadosPf(
            Long pessoaCadastroUnicoId,
            PesPessoa pessoa,
            Integer estadoCivil,
            Long cidadeNascimentoCcm) {

        atualizarTextoDadosPfSeVazio(
                pessoaCadastroUnicoId,
                "NOME_SOCIAL",
                pessoa.getNomeSocial()
        );

        atualizarTextoDadosPfSeVazio(
                pessoaCadastroUnicoId,
                "SEXO",
                pessoa.getSexo()
        );

        atualizarTextoDadosPfSeVazio(
                pessoaCadastroUnicoId,
                "MAE",
                pessoa.getMae()
        );

        atualizarTextoDadosPfSeVazio(
                pessoaCadastroUnicoId,
                "PAI",
                pessoa.getPai()
        );

        if (estadoCivil != null) {

            manager.createNativeQuery("""
            update dbo_ccm_pessoas.dados_pf
               set estado_civil = :estadoCivil
             where id = :pessoaId
               and estado_civil is null
        """)
                    .setParameter(
                            "estadoCivil",
                            estadoCivil
                    )
                    .setParameter(
                            "pessoaId",
                            pessoaCadastroUnicoId
                    )
                    .executeUpdate();
        }

        if (cidadeNascimentoCcm != null) {

            manager.createNativeQuery("""
            update dbo_ccm_pessoas.dados_pf
               set local_nascimento_id = :cidadeNascimento
             where id = :pessoaId
               and local_nascimento_id is null
        """)
                    .setParameter(
                            "cidadeNascimento",
                            cidadeNascimentoCcm
                    )
                    .setParameter(
                            "pessoaId",
                            pessoaCadastroUnicoId
                    )
                    .executeUpdate();
        }

        if (pessoa.getDataNascimento() != null) {

            manager.createNativeQuery("""
            update dbo_ccm_pessoas.dados_pf
               set data_nascimento = :dataNascimento
             where id = :pessoaId
               and data_nascimento is null
        """)
                    .setParameter(
                            "dataNascimento",
                            pessoa.getDataNascimento()
                    )
                    .setParameter(
                            "pessoaId",
                            pessoaCadastroUnicoId
                    )
                    .executeUpdate();
        }
    }
    protected void atualizarTextoDadosPfSeVazio(
            Long pessoaCadastroUnicoId,
            String coluna,
            String valor) {

        if (valor == null || valor.isBlank()) {
            return;
        }

        manager.createNativeQuery("""
        update dbo_ccm_pessoas.dados_pf
           set %s = :valor
         where id = :pessoaId
           and (%s is null or trim(%s) = '')
    """.formatted(
                        coluna,
                        coluna,
                        coluna
                ))
                .setParameter(
                        "valor",
                        valor.trim()
                )
                .setParameter(
                        "pessoaId",
                        pessoaCadastroUnicoId
                )
                .executeUpdate();
    }
    protected void inserirEnderecoSeNaoExistir(
            Long pessoaCadastroUnicoId,
            PesPessoa pessoa) {

        EnderecoCarga endereco =
                resolverEnderecoCarga(
                        pessoa
                );

        if (endereco == null) {
            return;
        }

        if (existeEndereco(
                pessoaCadastroUnicoId,
                pessoa,
                endereco
        )) {
            return;
        }

        String principal =
                possuiEnderecoPessoa(
                        pessoaCadastroUnicoId
                )
                        ? "N"
                        : "S";

        inserirEnderecoResolvido(
                pessoaCadastroUnicoId,
                pessoa,
                endereco,
                principal,
                null
        );
    }
    protected boolean existeEndereco(
            Long pessoaCadastroUnicoId,
            PesPessoa pessoa,
            EnderecoCarga endereco) {

        Number quantidade =
                (Number) manager.createNativeQuery("""
                select count(1)
                  from dbo_ccm_pessoas.enderecos e
                 where e.pessoa_id = :pessoaId
                   and e.dados_pj_id is null
                   and nvl(e.bairro_id, -1) =
                       nvl(:bairroId, -1)
                   and nvl(e.logradouro_id, -1) =
                       nvl(:logradouroId, -1)
                   and nvl(e.numero, -1) =
                       nvl(:numero, -1)
            """)
                        .setParameter(
                                "pessoaId",
                                pessoaCadastroUnicoId
                        )
                        .setParameter(
                                "bairroId",
                                endereco.bairroId
                        )
                        .setParameter(
                                "logradouroId",
                                endereco.logradouroId
                        )
                        .setParameter(
                                "numero",
                                pessoa.getNumero()
                        )
                        .getSingleResult();

        return quantidade != null
                && quantidade.longValue() > 0;
    }
    protected boolean possuiEnderecoPessoa(
            Long pessoaCadastroUnicoId) {

        Number quantidade =
                (Number) manager.createNativeQuery("""
                select count(1)
                  from dbo_ccm_pessoas.enderecos e
                 where e.pessoa_id = :pessoaId
                   and e.dados_pj_id is null
            """)
                        .setParameter(
                                "pessoaId",
                                pessoaCadastroUnicoId
                        )
                        .getSingleResult();

        return quantidade != null
                && quantidade.longValue() > 0;
    }
    protected void inserirDocumentosSeNaoExistirem(
            Long pessoaCadastroUnicoId,
            PesPessoa pessoa) {

        Set<String> documentosExistentes =
                buscarChavesDocumentosExistentes(
                        pessoaCadastroUnicoId
                );

        /*
         * O método existente verifica as chaves do Set antes
         * de inserir o documento comum e o título eleitoral.
         */
        inserirDocumentosPessoa(
                pessoaCadastroUnicoId,
                pessoa,
                documentosExistentes
        );
    }
    protected Set<String> buscarChavesDocumentosExistentes(
            Long pessoaCadastroUnicoId) {

        List<?> resultados =
                manager.createNativeQuery("""
                select
                    d.tipo_documento,
                    d.numero_documento
                  from dbo_ccm_pessoas.documentos d
                 where d.pessoa_id = :pessoaId
                   and d.dados_pj_id is null
                   and d.tipo_documento is not null
                   and d.numero_documento is not null
            """)
                        .setParameter(
                                "pessoaId",
                                pessoaCadastroUnicoId
                        )
                        .getResultList();

        Set<String> documentosExistentes =
                new HashSet<>();

        for (Object resultado : resultados) {

            Object[] registro =
                    (Object[]) resultado;

            if (registro[0] == null
                    || registro[1] == null) {
                continue;
            }

            Integer tipoDocumento =
                    ((Number) registro[0])
                            .intValue();

            String numeroDocumento =
                    registro[1]
                            .toString()
                            .trim();

            if (numeroDocumento.isBlank()) {
                continue;
            }

            String chave =
                    tipoDocumento
                            + "|"
                            + numeroDocumento.toUpperCase();

            documentosExistentes.add(
                    chave
            );
        }

        return documentosExistentes;
    }
    protected void inserirContatosSeNaoExistirem(
            Long pessoaCadastroUnicoId,
            PesPessoa pessoa) {

        Set<String> contatosExistentes =
                buscarChavesContatosExistentes(
                        pessoaCadastroUnicoId
                );

        inserirContatoSeNaoExistir(
                pessoaCadastroUnicoId,
                0L,
                pessoa.getTelefone(),
                contatosExistentes
        );

        inserirContatoSeNaoExistir(
                pessoaCadastroUnicoId,
                1L,
                pessoa.getCelular(),
                contatosExistentes
        );

        inserirContatoSeNaoExistir(
                pessoaCadastroUnicoId,
                3L,
                pessoa.getEmail(),
                contatosExistentes
        );

        inserirContatoSeNaoExistir(
                pessoaCadastroUnicoId,
                4L,
                pessoa.getPaginaWeb(),
                contatosExistentes
        );

        inserirContatoSeNaoExistir(
                pessoaCadastroUnicoId,
                5L,
                pessoa.getRecado(),
                contatosExistentes
        );

        inserirContatoSeNaoExistir(
                pessoaCadastroUnicoId,
                6L,
                pessoa.getFax(),
                contatosExistentes
        );
    }
    protected void inserirContatoSeNaoExistir(
            Long pessoaCadastroUnicoId,
            Long tipoContato,
            Object contato,
            Set<String> contatosExistentes) {

        if (contato == null) {
            return;
        }

        String valor =
                contato.toString().trim();

        if (valor.isBlank()
                || "0".equals(valor)) {
            return;
        }

        String chave =
                tipoContato
                        + "|"
                        + valor.toUpperCase();

        if (!contatosExistentes.add(chave)) {
            return;
        }

        inserirContato(
                pessoaCadastroUnicoId,
                tipoContato,
                contato
        );
    }
    protected Set<String> buscarChavesContatosExistentes(
            Long pessoaCadastroUnicoId) {

        List<?> resultados =
                manager.createNativeQuery("""
                select
                    c.tipo_contato,
                    c.contato
                  from dbo_ccm_pessoas.contatos c
                 where c.pessoa_id = :pessoaId
                   and c.dados_pj_id is null
                   and c.tipo_contato is not null
                   and c.contato is not null
            """)
                        .setParameter(
                                "pessoaId",
                                pessoaCadastroUnicoId
                        )
                        .getResultList();

        Set<String> contatosExistentes =
                new HashSet<>();

        for (Object resultado : resultados) {

            Object[] registro =
                    (Object[]) resultado;

            if (registro[0] == null
                    || registro[1] == null) {
                continue;
            }

            Long tipoContato =
                    ((Number) registro[0])
                            .longValue();

            String contato =
                    registro[1]
                            .toString()
                            .trim();

            if (contato.isBlank()
                    || "0".equals(contato)) {
                continue;
            }

            String chave =
                    tipoContato
                            + "|"
                            + contato.toUpperCase();

            contatosExistentes.add(
                    chave
            );
        }

        return contatosExistentes;
    }

}