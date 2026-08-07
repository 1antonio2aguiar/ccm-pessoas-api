package br.com.cc.pessoas.unificacao.pessoa.pesService;

import br.com.cc.pessoas.unificacao.pessoa.pesDto.PesPessoaDTO;
import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pessoa.pesFilter.PesPessoaFilter;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PesPessoaService extends PesPessoaBaseService {
    @Autowired
    private ProcessarEmpresasVinculadas processarEmpresasVinculadas;
    @Autowired
    private TransactionTemplate transactionTemplate;
    private static final int TAMANHO_LOTE = 200;

    /**
     * Inicia uma nova carga de CPF único.
     */
    public Long iniciarCargaCpfUnico() {

        Long idControle = iniciarControle();

        executarCargaCpfUnico(idControle);

        return idControle;
    }

    /**
     * Executa o processamento em lote dos CPFs únicos ainda não migrados.
     *
     * A consulta sempre busca a página zero porque, a cada processamento,
     * o registro passa a não aparecer mais na consulta de não migrados.
     *
     * Caso incrementássemos a página, alguns registros poderiam ser pulados.
     */
    public void executarCargaCpfUnico(Long idControle) {

        try {
            marcarIniciado(idControle);

            while (true) {

                PesPessoaFilter filter =
                        new PesPessoaFilter();

                Page<PesPessoaDTO> page =
                        pesPessoaRepository
                                .filtrarCpfUnicoNaoMigradas(
                                        filter,
                                        PageRequest.of(
                                                0,
                                                TAMANHO_LOTE
                                        )
                                );

                if (page.isEmpty()) {
                    break;
                }

                for (PesPessoaDTO pessoaDTO : page.getContent()) {

                    try {

                        Boolean processado =
                                transactionTemplate.execute(
                                        status ->
                                                processarCpfUnicoPorId(
                                                        pessoaDTO.pessoa()
                                                )
                                );

                        if (Boolean.TRUE.equals(processado)) {
                            somarProcessado(idControle);
                        }

                    } catch (Exception e) {

                        somarErro(
                                idControle,
                                montarMensagemErro(
                                        pessoaDTO,
                                        e
                                )
                        );
                    }
                }
            }

            marcarFinalizado(idControle);

        } catch (Exception e) {
            marcarErroGeral(
                    idControle,
                    obterMensagemErro(e)
            );

            throw e;
        }
    }

    /**
     * Processamento unitário chamado pelo endpoint.
     */
    @Transactional
    public boolean processarCpfUnicoPorId(
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

        return processarCpfUnico(pessoa);
    }

    /**
     * Realiza a migração de uma pessoa física com CPF único.
     */
    protected boolean processarCpfUnico(
            PesPessoa pessoa) {

        validarPessoaFisica(pessoa);

        String cpf =
                normalizarCpf(
                        pessoa.getCnpjCpf()
                );

        if (cpf == null || cpf.isBlank()) {
            throw new RuntimeException(
                    "CPF não informado para a pessoa "
                            + pessoa.getPessoa()
                            + "."
            );
        }

        /*
         * Proteção adicional.
         *
         * A própria consulta filtrarCpfUnicoNaoMigradas já elimina
         * CPFs repetidos na origem, mas fazemos novamente a validação
         * para proteger o processamento unitário.
         */
        validarCpfNaoDuplicado(pessoa);

        /*
         * Caso o CPF já esteja no Cadastro Único, não cria outra pessoa.
         */
        if (cpfJaExisteNoDadosPf(cpf)) {
            return false;
        }

        Long idPessoa = getNextVal("SEQ_PESSOAS");

        Long cidadeNascimentoCcm =
                buscarCidadeNascimentoCcm(
                        pessoa.getCidadeNascimento()
                );

        Integer estadoCivil =
                mapearEstadoCivil(
                        pessoa.getEstadoCivil()
                );

        Long tipoPessoa =
                getTipoPessoa(pessoa);

        inserirPessoa(
                idPessoa,
                pessoa,
                tipoPessoa
        );

        inserirCadUnicoPessoaFisica(
                idPessoa,
                pessoa,
                tipoPessoa,
                cpf,
                cidadeNascimentoCcm
        );

        inserirDadosPf(
                idPessoa,
                pessoa,
                cpf,
                estadoCivil,
                cidadeNascimentoCcm
        );

        inserirEnderecoPrincipal(
                idPessoa,
                pessoa, null
        );


        inserirDocumentosPessoa(
                idPessoa,
                pessoa
        );

        inserirContatos(
                pessoa,
                idPessoa
        );

        /*
         * Depois de migrar a pessoa física, verifica se existem
         * empresas vinculadas ao sócio na origem.
         */

        processarEmpresasVinculadas.processar(
                pessoa,
                idPessoa
        );

        return true;
    }

    /**
     * Valida se o registro é uma pessoa física.
     */
    protected void validarPessoaFisica(
            PesPessoa pessoa) {

        if (pessoa == null) {
            throw new IllegalArgumentException(
                    "Pessoa não informada."
            );
        }

        if (!"F".equalsIgnoreCase(
                pessoa.getFisicaJuridica()
        )) {
            throw new RuntimeException(
                    "O registro "
                            + pessoa.getPessoa()
                            + " não é uma pessoa física."
            );
        }
    }

    /**
     * Confirma que o CPF não está repetido na tabela de origem.
     */
    protected void validarCpfNaoDuplicado(
            PesPessoa pessoa) {

        if (pessoa.getCgcCpf() == null
                || pessoa.getCgcCpf() == 0L) {

            throw new RuntimeException(
                    "CPF não informado para a pessoa "
                            + pessoa.getPessoa()
                            + "."
            );
        }

        Number quantidade =
                (Number) manager.createNativeQuery("""
                    select count(1)
                      from dbo_ccm_pessoas.pes_pessoas p
                     where p.fisica_juridica = 'F'
                       and p.cgc_cpf = :cpf
                       and p.pessoa <> :pessoa
                    """)
                        .setParameter(
                                "cpf",
                                pessoa.getCgcCpf()
                        )
                        .setParameter(
                                "pessoa",
                                pessoa.getPessoa()
                        )
                        .getSingleResult();

        if (quantidade != null
                && quantidade.longValue() > 0L) {

            throw new RuntimeException(
                    "CPF duplicado na origem. "
                            + "Utilize a rotina de CPF duplicado."
            );
        }
    }

    /**
     * Monta a mensagem registrada no controle da carga.
     */
    private String montarMensagemErro(
            PesPessoaDTO pessoa,
            Exception erro) {

        StringBuilder mensagem =
                new StringBuilder();

        mensagem.append("Pessoa ");

        if (pessoa != null
                && pessoa.pessoa() != null) {

            mensagem.append(
                    pessoa.pessoa()
            );

        } else {
            mensagem.append(
                    "não identificada"
            );
        }

        if (pessoa != null
                && pessoa.nome() != null
                && !pessoa.nome().isBlank()) {

            mensagem.append(" - ");
            mensagem.append(
                    pessoa.nome().trim()
            );
        }

        mensagem.append(": ");
        mensagem.append(
                obterMensagemErro(erro)
        );

        return mensagem.toString();
    }

    /**
     * Obtém uma mensagem útil mesmo quando a exceção não possui mensagem.
     */
    private String obterMensagemErro(
            Throwable erro) {

        if (erro == null) {
            return "Erro não identificado.";
        }

        Throwable atual = erro;

        while (atual.getCause() != null) {
            atual = atual.getCause();
        }

        if (atual.getMessage() != null
                && !atual.getMessage().isBlank()) {

            return atual.getMessage();
        }

        return atual.getClass().getSimpleName();
    }
}