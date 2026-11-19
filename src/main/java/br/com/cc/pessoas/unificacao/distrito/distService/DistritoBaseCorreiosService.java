package br.com.cc.pessoas.unificacao.distrito.distService;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoBaseCorreiosResultadoDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoBaseCorreiosResumoDTO;
import br.com.cc.pessoas.unificacao.distrito.distRepository.DistritoBaseCorreiosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Base dos Correios da unificação de distritos.
 *
 * - gerar: incremental, só acrescenta os distritos que faltam;
 * - zerar: apaga DISTRITOS_UNIFICADO e DISTRITOS_UNIFICADO_CTRL
 *   e gera a base novamente (fase de testes).
 *
 * Usa a mesma chave de cidades para desligar o zerar:
 *   unificacao.cidades.permitir-zerar=false
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistritoBaseCorreiosService {

    public static final String TEXTO_CONFIRMACAO = "ZERAR";

    private final DistritoBaseCorreiosRepository repository;

    @Value("${unificacao.cidades.permitir-zerar:true}")
    private boolean permitirZerar;

    @Transactional(readOnly = true)
    public DistritoBaseCorreiosResumoDTO buscarResumo() {
        return new DistritoBaseCorreiosResumoDTO(
                repository.contarCorreios(),
                repository.contarCorreiosFicticios(),
                repository.contarUnificado(),
                repository.contarFaltandoNaBase(),
                permitirZerar
        );
    }

    @Transactional
    public DistritoBaseCorreiosResultadoDTO gerar() {

        long inseridos = repository.inserirFaltantes();

        return new DistritoBaseCorreiosResultadoDTO(
                0L,
                0L,
                inseridos,
                inseridos == 0
                        ? "A base já estava completa. Nenhum distrito inserido."
                        : inseridos + " distrito(s) dos Correios inserido(s) na base."
        );
    }

    /*
     * Tudo em uma única transação: se qualquer passo falhar, nada é apagado.
     */
    @Transactional
    public DistritoBaseCorreiosResultadoDTO zerar(String confirmacao) {

        if (!permitirZerar) {
            throw new NegocioException(
                    "A opção de recomeçar do zero está desabilitada neste ambiente."
            );
        }

        if (confirmacao == null
                || !TEXTO_CONFIRMACAO.equals(confirmacao.trim().toUpperCase())) {
            throw new NegocioException(
                    "Confirmação inválida. Digite " + TEXTO_CONFIRMACAO + " para confirmar."
            );
        }

        long controle = repository.apagarControle();
        long unificado = repository.apagarUnificado();
        long inseridos = repository.inserirFaltantes();

        log.warn(
                "Unificação de distritos zerada: {} linhas de DISTRITOS_UNIFICADO e "
                        + "{} de controle apagadas; {} distritos dos Correios inseridos.",
                unificado,
                controle,
                inseridos
        );

        return new DistritoBaseCorreiosResultadoDTO(
                unificado,
                controle,
                inseridos,
                "Base de distritos recomeçada do zero. Apagados: "
                        + unificado + " distrito(s) unificado(s) e "
                        + controle + " registro(s) de controle."
                        + " Inseridos: " + inseridos + " distrito(s) dos Correios."
        );
    }
}
