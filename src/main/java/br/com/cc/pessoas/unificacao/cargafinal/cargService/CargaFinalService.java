package br.com.cc.pessoas.unificacao.cargafinal.cargService;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.cargafinal.cargDto.CargaFinalResultadoDTO;
import br.com.cc.pessoas.unificacao.cargafinal.cargDto.CargaFinalResumoDTO;
import br.com.cc.pessoas.unificacao.cargafinal.cargDto.NivelCargaFinalDTO;
import br.com.cc.pessoas.unificacao.cargafinal.cargEnum.NivelCargaFinal;
import br.com.cc.pessoas.unificacao.cargafinal.cargRepository.CargaFinalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Carga final (fase 3). Cada nível em uma transação: ou carrega tudo
 * nos dois schemas e grava o CODIGO_CCM, ou não grava nada.
 * "Carregar tudo" segue a ordem e para no primeiro nível com erro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CargaFinalService {

    private final CargaFinalRepository repository;
    private final TransactionTemplate transactionTemplate;

    public List<NivelCargaFinalDTO> listarNiveis() {
        return Arrays.stream(NivelCargaFinal.values())
                .map(NivelCargaFinalDTO::fromEnum)
                .toList();
    }

    @Transactional(readOnly = true)
    public CargaFinalResumoDTO buscarResumo(String nivel) {

        NivelCargaFinal n = NivelCargaFinal.from(nivel);
        long[] c = repository.contar(n);

        return new CargaFinalResumoDTO(n.name(), n.getDescricao(), c[0], c[1], c[2], c[3], c[4], c[5]);
    }

    @Transactional
    public CargaFinalResultadoDTO executar(String nivel) {
        return executarInterno(NivelCargaFinal.from(nivel));
    }

    public List<CargaFinalResultadoDTO> executarTodos() {

        List<CargaFinalResultadoDTO> resultados = new ArrayList<>();

        for (NivelCargaFinal nivel : NivelCargaFinal.values()) {

            try {
                resultados.add(transactionTemplate.execute(status -> executarInterno(nivel)));
            } catch (Exception e) {

                log.error("Erro na carga final de {}.", nivel, e);

                resultados.add(new CargaFinalResultadoDTO(
                        nivel.name(),
                        nivel.getDescricao(),
                        0L, 0L, 0L, 0L,
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

    /*
     * Trava: as tabelas finais já estão carregadas e os endereços das pessoas
     * apontam para esses IDs. Muitas linhas sem CODIGO_CCM indicam que o código
     * foi apagado na consolidada; carregar criaria IDs novos (duplicados) para
     * endereços que já existem.
     */
    private static final double LIMITE_PENDENTES = 0.20;
    private static final long MINIMO_PARA_TRAVA = 1000L;

    private void validarPendentes(NivelCargaFinal nivel) {

        if (nivel == NivelCargaFinal.CIDADES) {
            return;
        }

        long[] c = repository.contar(nivel);
        long total = c[0];
        long semCodigo = total - c[1];

        if (total >= MINIMO_PARA_TRAVA && semCodigo > total * LIMITE_PENDENTES) {
            throw new NegocioException(
                    nivel.getDescricao() + ": " + semCodigo + " de " + total
                            + " linhas da consolidada estão sem CODIGO_CCM. Isso indica que o código"
                            + " foi apagado; a carga criaria IDs novos para endereços que já existem."
                            + " Carga não executada. Confira a seção 0 do script 11."
            );
        }
    }

    private CargaFinalResultadoDTO executarInterno(NivelCargaFinal nivel) {

        validarPendentes(nivel);

        long[] r = switch (nivel) {
            case CIDADES -> repository.carregarCidades();
            case DISTRITOS -> repository.carregarDistritos();
            case BAIRROS -> repository.carregarBairros();
            case LOGRADOUROS -> repository.carregarLogradouros();
            case CEPS -> repository.carregarCeps();
        };

        log.info(
                "Carga final de {}: {} códigos novos, {} reaproveitados, {} em DBO_CC_ENDERECOS, {} em DBO_CCM_PESSOAS.",
                nivel, r[0], r[1], r[2], r[3]
        );

        StringBuilder mensagem = new StringBuilder(nivel.getDescricao()).append(": ");

        if (nivel != NivelCargaFinal.CIDADES) {
            mensagem.append(r[0]).append(" código(s) CCM novo(s)");

            if (r[1] > 0) {
                mensagem.append(", ").append(r[1]).append(" reaproveitado(s)");
            }

            mensagem.append("; ");
        }

        mensagem.append(r[2]).append(" inserido(s) em DBO_CC_ENDERECOS e ")
                .append(r[3]).append(" em DBO_CCM_PESSOAS.");

        return new CargaFinalResultadoDTO(
                nivel.name(),
                nivel.getDescricao(),
                r[0], r[1], r[2], r[3],
                true,
                mensagem.toString()
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
