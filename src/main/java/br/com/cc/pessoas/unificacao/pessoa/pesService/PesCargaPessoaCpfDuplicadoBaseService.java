package br.com.cc.pessoas.unificacao.pessoa.pesService;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPessoa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

public abstract class PesCargaPessoaCpfDuplicadoBaseService
        extends PesPessoaBaseService {
    @Autowired
    private ProcessarEmpresasVinculadas processarEmpresasVinculadas;
    @Autowired
    private PesPessoaJaExisteCadUnicoService pessoaJaExisteCadUnicoService;
    protected abstract void insertDadosEspecificos(
            Long idPessoa,
            DadosGrupo dadosGrupo
    );
    /*
     * Guarda os melhores dados encontrados entre
     * os registros que fazem parte do grupo duplicado.
     */
    protected static class DadosGrupo extends PesPessoa {

        protected String cpfCnpjPrincipal;
        protected Long tipoPessoa;
        protected Long cidadeNascimentoCcm;
        protected Integer estadoCivilDestino;
    }
    protected void processarEmpresasDoGrupo(
            Long idPessoa,
            List<PesPessoa> grupo) {

        for (PesPessoa pessoaOrigem : grupo) {

            processarEmpresasVinculadas.processar(
                    pessoaOrigem,
                    idPessoa
            );
        }
    }
    @Transactional
    public boolean processarCpfDuplicadoPorId(
            Long pessoaId) {

        if (pessoaId == null) {
            throw new IllegalArgumentException(
                    "Código da pessoa não informado."
            );
        }

        PesPessoa pessoaSelecionada =
                pesPessoaRepository
                        .findById(pessoaId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Pessoa não encontrada: "
                                                + pessoaId
                                )
                        );

        if (!"F".equalsIgnoreCase(
                pessoaSelecionada.getFisicaJuridica()
        )) {
            throw new RuntimeException(
                    "O registro "
                            + pessoaId
                            + " não é uma pessoa física."
            );
        }

        String cpf =
            normalizarCpf(
                    pessoaSelecionada.getCnpjCpf()
            );

        if (cpf == null || cpf.isBlank()) {
            throw new RuntimeException(
                    "CPF não informado para a pessoa "
                            + pessoaId
                            + "."
            );
        }

        if (cpfJaExisteNoDadosPf(cpf)) {
            return false;
        }

        List<PesPessoa> grupo =
                buscarGrupoDuplicado(
                        pessoaId
                );

        if (grupo.size() < 2) {
            throw new RuntimeException(
                    "Não foi encontrado um grupo de CPF duplicado "
                            + "para a pessoa "
                            + pessoaId
                            + "."
            );
        }

        processarGrupo(grupo);

        return true;
    }
    @Transactional
    public void processarCpfDuplicadoJaExistePorId(
            Long pessoaId) {

        if (pessoaId == null) {
            throw new IllegalArgumentException(
                    "Código da pessoa não informado."
            );
        }

        PesPessoa pessoaSelecionada =
                pesPessoaRepository
                        .findById(pessoaId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Pessoa não encontrada: "
                                                + pessoaId
                                )
                        );

        if (!"F".equalsIgnoreCase(
                pessoaSelecionada.getFisicaJuridica()
        )) {
            throw new RuntimeException(
                    "O registro "
                            + pessoaId
                            + " não é uma pessoa física."
            );
        }

        String cpf =
                normalizarCpf(
                        pessoaSelecionada.getCnpjCpf()
                );

        if (cpf == null || cpf.isBlank()) {
            throw new RuntimeException(
                    "CPF não informado para a pessoa "
                            + pessoaId
                            + "."
            );
        }

        Long pessoaCadastroUnicoId =
                pessoaJaExisteCadUnicoService
                        .buscarPessoaExistentePorCpf(
                                cpf
                        );

        if (pessoaCadastroUnicoId == null) {
            throw new RuntimeException(
                    "O CPF "
                            + cpf
                            + " não foi localizado no Cadastro Único."
            );
        }

        List<PesPessoa> grupo =
                buscarGrupoDuplicado(
                        pessoaId
                );

        if (grupo.size() < 2) {
            throw new RuntimeException(
                    "Não foi encontrado um grupo de CPF duplicado "
                            + "para a pessoa "
                            + pessoaId
                            + "."
            );
        }

        processarGrupoJaExistente(
                pessoaCadastroUnicoId,
                grupo
        );
    }
    protected void processarGrupoJaExistente(
            Long pessoaCadastroUnicoId,
            List<PesPessoa> grupo) {

        DadosGrupo dadosGrupo =
                montarDadosGrupo(
                        grupo
                );

        /*
         * Cria um vínculo em CAD_UNICO_PESSOA para
         * cada registro de origem do grupo.
         */
        for (PesPessoa pessoaOrigem : grupo) {

            if (pessoaJaExisteCadUnicoService
                    .jaExisteVinculo(
                            pessoaOrigem.getPessoa()
                    )) {
                continue;
            }

            Long tipoPessoaOrigem =
                    getTipoPessoa(
                            pessoaOrigem
                    );

            inserirCadUnicoPessoaFisica(
                    pessoaCadastroUnicoId,
                    pessoaOrigem,
                    tipoPessoaOrigem,
                    dadosGrupo.cpfCnpjPrincipal,
                    dadosGrupo.cidadeNascimentoCcm
            );
        }

        /*
         * Complementa uma única vez os dados consolidados.
         */
        pessoaJaExisteCadUnicoService
                .complementarPessoa(
                        pessoaCadastroUnicoId,
                        dadosGrupo,
                        dadosGrupo.tipoPessoa
                );

        pessoaJaExisteCadUnicoService
                .complementarDadosPf(
                        pessoaCadastroUnicoId,
                        dadosGrupo,
                        dadosGrupo.estadoCivilDestino,
                        dadosGrupo.cidadeNascimentoCcm
                );

        /*
         * Cada integrante pode possuir informações diferentes.
         * Os métodos verificam o banco antes de inserir.
         */
        for (PesPessoa pessoaOrigem : grupo) {

            pessoaJaExisteCadUnicoService
                    .inserirEnderecoSeNaoExistir(
                            pessoaCadastroUnicoId,
                            pessoaOrigem
                    );

            pessoaJaExisteCadUnicoService
                    .inserirDocumentosSeNaoExistirem(
                            pessoaCadastroUnicoId,
                            pessoaOrigem
                    );

            pessoaJaExisteCadUnicoService
                    .inserirContatosSeNaoExistirem(
                            pessoaCadastroUnicoId,
                            pessoaOrigem
                    );
        }

        /*
         * Processa as empresas vinculadas a todos
         * os integrantes do grupo.
         */
        processarEmpresasDoGrupo(
                pessoaCadastroUnicoId,
                grupo
        );
    }
    protected void processarGrupo(
            List<PesPessoa> grupo) {

        if (grupo == null || grupo.isEmpty()) {
            throw new RuntimeException(
                    "Grupo de CPF duplicado não informado."
            );
        }

        DadosGrupo dadosGrupo =
                montarDadosGrupo(grupo);

        Long idPessoa =
                getNextVal("SEQ_PESSOAS");

        /*
         * Uma única pessoa principal.
         */
        inserirPessoa(
                idPessoa,
                dadosGrupo,
                dadosGrupo.tipoPessoa
        );

        /*
         * Uma origem em CAD_UNICO_PESSOA para cada
         * registro repetido.
         */
        for (PesPessoa pessoaOrigem : grupo) {

            Long tipoPessoaOrigem =
                    getTipoPessoa(
                            pessoaOrigem
                    );

            inserirCadUnicoPessoaFisica(
                    idPessoa,
                    pessoaOrigem,
                    tipoPessoaOrigem,
                    dadosGrupo.cpfCnpjPrincipal,
                    dadosGrupo.cidadeNascimentoCcm
            );
        }

        /*
         * Um único DADOS_PF consolidado.
         */
        insertDadosEspecificos(
                idPessoa,
                dadosGrupo
        );

        inserirEnderecosUnicos(
                idPessoa,
                grupo
        );

        inserirContatosUnicos(
                idPessoa,
                grupo
        );

        inserirDocumentosUnicos(
                idPessoa,
                grupo
        );

        processarEmpresasDoGrupo(
                idPessoa,
                grupo
        );

    }

    @SuppressWarnings("unchecked")
    protected List<PesPessoa> buscarGrupoDuplicado(
            Long pessoaId) {

        if (pessoaId == null) {
            return new ArrayList<>();
        }

        List<Number> resultados =
                manager.createNativeQuery("""
                select p.pessoa
                  from dbo_ccm_pessoas.pes_pessoas p
                 where p.fisica_juridica = 'F'
                     and p.cnpj_cpf is not null
                     and length(trim(p.cnpj_cpf)) = 11
                     and regexp_like(
                             trim(p.cnpj_cpf),
                             '^[0-9]{11}$'
                         )
                     and trim(p.cnpj_cpf) <> '00000000000'
                     and trim(p.cnpj_cpf) = (
                           select trim(p0.cnpj_cpf)
                             from dbo_ccm_pessoas.pes_pessoas p0
                            where p0.pessoa = :pessoaId
                              and p0.fisica_juridica = 'F'
                     )
                   and replace(
                           transf_caracte(p.nome),
                           ' ',
                           ''
                       ) = (
                        select replace(
                                   transf_caracte(p1.nome),
                                   ' ',
                                   ''
                               )
                          from dbo_ccm_pessoas.pes_pessoas p1
                         where p1.pessoa = :pessoaId
                           and p1.fisica_juridica = 'F'
                   )
                   and not exists (
                        select 1
                          from dbo_ccm_pessoas.cad_unico_pessoa cup
                         where cup.cd_origem = p.pessoa
                           and cup.banco = 'PESSOAS'
                   )
                 order by p.pessoa
            """)
                        .setParameter(
                                "pessoaId",
                                pessoaId
                        )
                        .getResultList();

        if (resultados == null || resultados.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> ids =
                resultados.stream()
                        .map(Number::longValue)
                        .toList();

        Map<Long, PesPessoa> pessoasPorId =
                pesPessoaRepository
                        .findAllById(ids)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        PesPessoa::getPessoa,
                                        Function.identity()
                                )
                        );

        return ids.stream()
                .map(pessoasPorId::get)
                .filter(pessoa -> pessoa != null)
                .toList();
    }
    protected DadosGrupo montarDadosGrupo(
            List<PesPessoa> grupo) {

        DadosGrupo dados = new DadosGrupo();
        StringBuilder observacoes = new StringBuilder();

        dados.setFisicaJuridica("F");

        for (PesPessoa pessoa : grupo) {

            if (dados.cpfCnpjPrincipal == null) {

                dados.cpfCnpjPrincipal =
                        normalizarCpf(
                                pessoa.getCnpjCpf()
                        );
            }

            if (dados.tipoPessoa == null) {
                dados.tipoPessoa =
                        getTipoPessoa(pessoa);
            }

            if (dados.getNome() == null
                    && pessoa.getNome() != null
                    && !pessoa.getNome().isBlank()) {

                dados.setNome(
                        pessoa.getNome()
                );
            }

            if (dados.getDataCadastro() == null
                    && pessoa.getDataCadastro() != null) {

                dados.setDataCadastro(
                        pessoa.getDataCadastro()
                );
            }

            if (dados.getNomeSocial() == null
                    && pessoa.getNomeSocial() != null
                    && !pessoa.getNomeSocial().isBlank()) {

                dados.setNomeSocial(
                        pessoa.getNomeSocial()
                );
            }

            if (dados.getSexo() == null
                    && pessoa.getSexo() != null
                    && !pessoa.getSexo().isBlank()) {

                dados.setSexo(
                        pessoa.getSexo()
                );
            }

            if (dados.estadoCivilDestino == null) {
                dados.estadoCivilDestino =
                        mapearEstadoCivil(
                                pessoa.getEstadoCivil()
                        );
            }

            if (dados.cidadeNascimentoCcm == null) {
                dados.cidadeNascimentoCcm =
                        buscarCidadeNascimentoCcm(
                                pessoa.getCidadeNascimento()
                        );
            }

            if (dados.getMae() == null
                    && pessoa.getMae() != null
                    && !pessoa.getMae().isBlank()) {

                dados.setMae(
                        pessoa.getMae()
                );
            }

            if (dados.getPai() == null
                    && pessoa.getPai() != null
                    && !pessoa.getPai().isBlank()) {

                dados.setPai(
                        pessoa.getPai()
                );
            }

            if (dados.getDataNascimento() == null
                    && pessoa.getDataNascimento() != null) {

                dados.setDataNascimento(
                        pessoa.getDataNascimento()
                );
            }

            if (dados.getEmail() == null
                    && pessoa.getEmail() != null
                    && !pessoa.getEmail().isBlank()) {

                dados.setEmail(
                        pessoa.getEmail()
                );
            }

            if (pessoa.getObservacao() != null
                    && !pessoa.getObservacao().isBlank()) {

                if (!observacoes.isEmpty()) {
                    observacoes.append(" ");
                }

                observacoes.append(
                        pessoa.getObservacao().trim()
                );
            }
        }

        if (!observacoes.isEmpty()) {
            dados.setObservacao(
                    observacoes.toString()
            );
        }

        return dados;
    }
    protected void inserirEnderecosUnicos(
            Long idPessoa,
            List<PesPessoa> grupo) {

        Set<String> enderecosInseridos =
                new HashSet<>();

        boolean principalDefinido = false;

        for (PesPessoa pessoa : grupo) {

            EnderecoCarga endereco =
                    resolverEnderecoCarga(pessoa);

            if (endereco == null) {
                continue;
            }

            String chave =
                    endereco.bairroId
                            + "|"
                            + endereco.logradouroId
                            + "|"
                            + pessoa.getNumero();

            if (!enderecosInseridos.add(chave)) {
                continue;
            }

            String principal =
                    principalDefinido
                            ? "N"
                            : "S";

            inserirEnderecoResolvido(
                    idPessoa,
                    pessoa,
                    endereco,
                    principal,
                    null
            );

            principalDefinido = true;
        }
    }
    protected void inserirContatosUnicos(
            Long idPessoa,
            List<PesPessoa> grupo) {

        Set<String> contatosInseridos =
                new HashSet<>();

        for (PesPessoa pessoa : grupo) {

            inserirContatoUnico(
                    idPessoa,
                    0L,
                    pessoa.getTelefone(),
                    contatosInseridos
            );

            inserirContatoUnico(
                    idPessoa,
                    1L,
                    pessoa.getCelular(),
                    contatosInseridos
            );

            inserirContatoUnico(
                    idPessoa,
                    3L,
                    pessoa.getEmail(),
                    contatosInseridos
            );

            inserirContatoUnico(
                    idPessoa,
                    4L,
                    pessoa.getPaginaWeb(),
                    contatosInseridos
            );

            inserirContatoUnico(
                    idPessoa,
                    5L,
                    pessoa.getRecado(),
                    contatosInseridos
            );

            inserirContatoUnico(
                    idPessoa,
                    6L,
                    pessoa.getFax(),
                    contatosInseridos
            );
        }
    }
    private void inserirContatoUnico(
            Long idPessoa,
            Long tipoContato,
            Object contato,
            Set<String> contatosInseridos) {

        if (contato == null) {
            return;
        }

        String valor =
                String.valueOf(contato).trim();

        if (valor.isBlank()
                || "0".equals(valor)) {
            return;
        }

        String chave =
                tipoContato
                        + "|"
                        + valor.toUpperCase();

        if (!contatosInseridos.add(chave)) {
            return;
        }

        inserirContato(
                idPessoa,
                tipoContato,
                contato
        );
    }
    protected void inserirDocumentosUnicos(
            Long idPessoa,
            List<PesPessoa> grupo) {

        Set<String> documentosInseridos =
                new HashSet<>();

        for (PesPessoa pessoa : grupo) {

            inserirDocumentosPessoa(
                    idPessoa,
                    pessoa,
                    documentosInseridos
            );
        }
    }
}