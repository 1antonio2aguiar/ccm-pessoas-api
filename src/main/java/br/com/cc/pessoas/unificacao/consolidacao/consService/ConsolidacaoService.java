package br.com.cc.pessoas.unificacao.consolidacao.consService;

import br.com.cc.pessoas.unificacao.consolidacao.consDto.ConsolidacaoResultadoDTO;
import br.com.cc.pessoas.unificacao.consolidacao.consDto.ConsolidacaoResumoDTO;
import br.com.cc.pessoas.unificacao.consolidacao.consDto.NivelConsolidacaoDTO;
import br.com.cc.pessoas.unificacao.consolidacao.consEnum.NivelConsolidacao;
import br.com.cc.pessoas.unificacao.consolidacao.consRepository.ConsolidacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Consolidação (fase 2): *_UNIFICADO -> DBO_UNI_PESSOAS.CIDADES / DISTRITOS /
 * BAIRROS / LOGRADOUROS / CEPS.
 *
 * Cada nível roda em uma transação: ou consolida tudo, ou nada.
 * "Executar todos" segue a ordem cidades -> distritos -> bairros ->
 * logradouros -> CEPs e para no primeiro nível com erro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsolidacaoService {

    private final ConsolidacaoRepository repository;
    private final TransactionTemplate transactionTemplate;

    public List<NivelConsolidacaoDTO> listarNiveis() {
        return Arrays.stream(NivelConsolidacao.values())
                .map(NivelConsolidacaoDTO::fromEnum)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsolidacaoResumoDTO buscarResumo(String nivel) {

        NivelConsolidacao n = NivelConsolidacao.from(nivel);

        return new ConsolidacaoResumoDTO(
                n.name(),
                n.getDescricao(),
                n.getTabelaDestino(),
                repository.contarUnificado(n),
                repository.contarConsolidado(n),
                repository.contarFaltando(n),
                repository.contarSoConsolidado(n)
        );
    }

    @Transactional
    public ConsolidacaoResultadoDTO executar(String nivel) {
        return executarInterno(NivelConsolidacao.from(nivel));
    }

    public List<ConsolidacaoResultadoDTO> executarTodos() {

        List<ConsolidacaoResultadoDTO> resultados = new ArrayList<>();

        for (NivelConsolidacao nivel : NivelConsolidacao.values()) {

            try {
                resultados.add(transactionTemplate.execute(status -> executarInterno(nivel)));
            } catch (Exception e) {

                log.error("Erro ao consolidar {}.", nivel, e);

                resultados.add(new ConsolidacaoResultadoDTO(
                        nivel.name(),
                        nivel.getDescricao(),
                        0L,
                        0L,
                        false,
                        "Erro: " + obterMensagemErro(e)
                                + " Nenhuma alteração foi feita neste nível;"
                                + " os níveis seguintes não foram executados."
                ));

                break;
            }
        }

        return resultados;
    }

    private ConsolidacaoResultadoDTO executarInterno(NivelConsolidacao nivel) {

        long faltando = repository.contarFaltando(nivel);
        long afetados = repository.mesclar(nivel);
        long completados = Math.max(afetados - faltando, 0L);

        log.info("Consolidação de {}: {} inseridos, {} completados.", nivel, faltando, completados);

        return new ConsolidacaoResultadoDTO(
                nivel.name(),
                nivel.getDescricao(),
                faltando,
                completados,
                true,
                nivel.getDescricao() + ": " + faltando + " linha(s) inserida(s) e "
                        + completados + " linha(s) completada(s) em "
                        + nivel.getTabelaDestino() + "."
        );
    }

    private String obterMensagemErro(Throwable erro) {

        if (erro == null) {
            return "Erro não identificado.";
        }

        Throwable atual = erro;

        while (atual.getCause() != null) {
            atual = atual.getCause();
        }

        if (atual.getMessage() != null && !atual.getMessage().isBlank()) {
            return atual.getMessage().trim();
        }

        return atual.getClass().getSimpleName();
    }
}
