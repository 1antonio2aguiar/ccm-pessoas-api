package br.com.cc.pessoas.unificacao.cidade.cidService;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeCandidatoDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeOrigemDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeUnificadoLoteDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.OrigemCidadeDTO;
import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import br.com.cc.pessoas.unificacao.cidade.cidFilter.CidadeUnificadoFilter;
import br.com.cc.pessoas.unificacao.cidade.cidRepository.CidadeUnificadoRepository;
import br.com.cc.pessoas.unificacao.cidade.cidRepository.CidadeUnificadoRepository.CidadeOrigemDados;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Unificação de cidades por sistema de origem.
 *
 * Substitui a procedure SP_CIDADES_GERA_UNIFICADO com processamento
 * incremental: nada é apagado, códigos já gravados não mudam e
 * cada cidade é tratada em sua própria transação.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CidadeUnificadoService {

    private static final int MAX_ERROS_RETORNADOS = 50;

    private final CidadeUnificadoRepository repository;
    private final TransactionTemplate transactionTemplate;

    public List<OrigemCidadeDTO> listarOrigens() {
        return Arrays.stream(OrigemCidade.values())
                .map(OrigemCidadeDTO::fromEnum)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CidadeOrigemDTO> filtrarPendentes(
            String origem,
            CidadeUnificadoFilter filter,
            Pageable pageable) {

        return repository.filtrarPendentes(
                OrigemCidade.from(origem),
                filter,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public CidadeUnificadoResumoDTO buscarResumo(
            String origem) {

        return repository.buscarResumo(
                OrigemCidade.from(origem)
        );
    }

    @Transactional(readOnly = true)
    public List<CidadeCandidatoDTO> buscarCandidatos(
            String origem,
            Long cidade,
            String pesquisa,
            String uf) {

        OrigemCidade origemCidade = OrigemCidade.from(origem);

        CidadeOrigemDados dados =
                buscarCidadeOrigem(origemCidade, cidade);

        return repository.buscarCandidatos(
                origemCidade,
                dados,
                pesquisa,
                uf
        );
    }

    /**********************************************************************
     * VINCULAR
     **********************************************************************/

    @Transactional
    public String vincular(
            String origem,
            Long cidade,
            Long idUnificado) {

        OrigemCidade origemCidade = OrigemCidade.from(origem);

        boolean duplicado =
                vincularInterno(
                        origemCidade,
                        cidade,
                        idUnificado
                );

        if (duplicado) {
            return "Cidade " + cidade
                    + " registrada como código duplicado da cidade unificada "
                    + idUnificado
                    + ", que já possui outro código desta origem.";
        }

        return "Cidade " + cidade
                + " vinculada à cidade unificada "
                + idUnificado + ".";
    }

    /*
     * Vincula todas as cidades pendentes com situação UNICA.
     *
     * A lista é lida uma única vez e cada cidade é gravada
     * em uma transação própria: um erro não desfaz as demais
     * e não existe risco de laço infinito.
     */
    public CidadeUnificadoLoteDTO vincularUnicas(String origem) {

        OrigemCidade origemCidade = OrigemCidade.from(origem);

        List<Long[]> unicas =
                repository.listarUnicas(origemCidade);

        long vinculados = 0;
        long duplicados = 0;
        List<String> erros = new ArrayList<>();
        long totalErros = 0;

        for (Long[] item : unicas) {

            Long cidade = item[0];
            Long idUnificado = item[1];

            try {

                Boolean duplicado =
                        transactionTemplate.execute(status ->
                                vincularInterno(
                                        origemCidade,
                                        cidade,
                                        idUnificado
                                )
                        );

                if (Boolean.TRUE.equals(duplicado)) {
                    duplicados++;
                } else {
                    vinculados++;
                }

            } catch (Exception e) {

                totalErros++;

                log.error(
                        "Erro ao vincular a cidade {} da origem {}.",
                        cidade,
                        origemCidade,
                        e
                );

                if (erros.size() < MAX_ERROS_RETORNADOS) {
                    erros.add(
                            "Cidade " + cidade + ": "
                                    + obterMensagemErro(e)
                    );
                }
            }
        }

        return new CidadeUnificadoLoteDTO(
                (long) unicas.size(),
                vinculados,
                duplicados,
                totalErros,
                erros
        );
    }

    /*
     * Retorna true quando a cidade unificada já tinha outro código
     * desta origem e o vínculo foi registrado como duplicado.
     */
    protected boolean vincularInterno(
            OrigemCidade origem,
            Long cidade,
            Long idUnificado) {

        if (idUnificado == null) {
            throw new NegocioException(
                    "Cidade unificada não informada."
            );
        }

        CidadeOrigemDados dados =
                buscarCidadeOrigem(origem, cidade);

        validarNaoTratada(origem, cidade);

        String ufUnificado =
                repository.buscarUfUnificado(idUnificado);

        if (ufUnificado == null) {
            throw new NegocioException(
                    "Cidade unificada " + idUnificado
                            + " não encontrada."
            );
        }

        if (!ufUnificado.equals(dados.uf())) {
            throw new NegocioException(
                    "A cidade unificada está em "
                            + ufUnificado
                            + " e a cidade de origem em "
                            + dados.uf()
                            + "."
            );
        }

        int alterados =
                repository.vincularCidade(
                        origem,
                        idUnificado,
                        dados
                );

        if (alterados > 0) {
            return false;
        }

        /*
         * A cidade unificada já possui outro código desta origem
         * (cidade duplicada no sistema de origem).
         * O código não é sobrescrito: fica registrado no controle.
         */
        repository.registrarControle(
                origem,
                cidade,
                CidadeUnificadoRepository.STATUS_DUPLICADO,
                idUnificado,
                "Código duplicado na origem.",
                null
        );

        return true;
    }

    /**********************************************************************
     * CRIAR CIDADE NOVA
     **********************************************************************/

    @Transactional
    public String criar(
            String origem,
            Long cidade) {

        OrigemCidade origemCidade = OrigemCidade.from(origem);

        CidadeOrigemDados dados =
                buscarCidadeOrigem(origemCidade, cidade);

        validarNaoTratada(origemCidade, cidade);

        List<CidadeCandidatoDTO> candidatos =
                repository.buscarCandidatos(
                        origemCidade,
                        dados,
                        null,
                        null
                );

        if (!candidatos.isEmpty()) {
            throw new NegocioException(
                    "Já existe(m) "
                            + candidatos.size()
                            + " cidade(s) correspondente(s) para "
                            + dados.nome() + "/" + dados.uf()
                            + ". Utilize a opção de vincular."
            );
        }

        Long idUnificado =
                repository.criarCidade(
                        origemCidade,
                        dados
                );

        return "Cidade " + dados.nome() + "/" + dados.uf()
                + " criada (cidade unificada " + idUnificado + ").";
    }

    /**********************************************************************
     * NÃO VINCULAR
     **********************************************************************/

    @Transactional
    public String naoVincular(
            String origem,
            Long cidade,
            String motivo) {

        OrigemCidade origemCidade = OrigemCidade.from(origem);

        buscarCidadeOrigem(origemCidade, cidade);

        validarNaoTratada(origemCidade, cidade);

        repository.registrarControle(
                origemCidade,
                cidade,
                CidadeUnificadoRepository.STATUS_NAO_VINCULAR,
                null,
                motivo == null || motivo.isBlank()
                        ? null
                        : motivo.trim(),
                null
        );

        return "Cidade " + cidade
                + " retirada da lista de unificação.";
    }

    /**********************************************************************
     * VALIDAÇÕES
     **********************************************************************/

    private CidadeOrigemDados buscarCidadeOrigem(
            OrigemCidade origem,
            Long cidade) {

        if (cidade == null) {
            throw new NegocioException(
                    "Código da cidade não informado."
            );
        }

        CidadeOrigemDados dados =
                repository.buscarCidadeOrigem(origem, cidade);

        if (dados == null) {
            throw new NegocioException(
                    "Cidade " + cidade
                            + " não encontrada na origem "
                            + origem.getDescricao() + "."
            );
        }

        return dados;
    }

    private void validarNaoTratada(
            OrigemCidade origem,
            Long cidade) {

        if (repository.origemJaTratada(origem, cidade)) {
            throw new NegocioException(
                    "A cidade " + cidade
                            + " da origem "
                            + origem.getDescricao()
                            + " já foi unificada ou possui decisão registrada."
            );
        }
    }

    private String obterMensagemErro(Throwable erro) {

        if (erro == null) {
            return "Erro não identificado.";
        }

        Throwable atual = erro;

        while (atual.getCause() != null) {
            atual = atual.getCause();
        }

        if (atual.getMessage() != null
                && !atual.getMessage().isBlank()) {
            return atual.getMessage().trim();
        }

        return atual.getClass().getSimpleName();
    }
}
