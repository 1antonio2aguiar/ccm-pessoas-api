package br.com.cc.pessoas.unificacao.bairro.bairService;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroBaseCorreiosResultadoDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroBaseCorreiosResumoDTO;
import br.com.cc.pessoas.unificacao.bairro.bairRepository.BairroBaseCorreiosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Base dos Correios da unificação de bairros.
 *
 * - gerar: incremental, só acrescenta os bairros que faltam;
 * - zerar: apaga BAIRROS_UNIFICADO e BAIRROS_UNIFICADO_CTRL
 *   e gera a base novamente (fase de testes).
 *
 * Usa a mesma chave de cidades para desligar o zerar:
 *   unificacao.cidades.permitir-zerar=false
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BairroBaseCorreiosService {

    public static final String TEXTO_CONFIRMACAO = "ZERAR";

    private final BairroBaseCorreiosRepository repository;

    @Value("${unificacao.cidades.permitir-zerar:true}")
    private boolean permitirZerar;

    @Transactional(readOnly = true)
    public BairroBaseCorreiosResumoDTO buscarResumo() {
        return new BairroBaseCorreiosResumoDTO(
                repository.contarCorreios(),
                repository.contarCorreiosFicticios(),
                repository.contarUnificado(),
                repository.contarFaltandoNaBase(),
                permitirZerar
        );
    }

    @Transactional
    public BairroBaseCorreiosResultadoDTO gerar() {

        long inseridos = repository.inserirFaltantes();
        long normalizados = repository.calcularNomeNormVazio();

        String mensagem = inseridos == 0
                ? "A base já estava completa. Nenhum bairro inserido."
                : inseridos + " bairro(s) dos Correios inserido(s) na base.";

        if (normalizados > 0) {
            mensagem += " " + normalizados + " nome(s) normalizado(s) calculado(s).";
        }

        return new BairroBaseCorreiosResultadoDTO(
                0L,
                0L,
                inseridos,
                mensagem
        );
    }

    /*
     * Tudo em uma única transação: se qualquer passo falhar, nada é apagado.
     */
    @Transactional
    public BairroBaseCorreiosResultadoDTO zerar(String confirmacao) {

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
                "Unificação de bairros zerada: {} linhas de BAIRROS_UNIFICADO e "
                        + "{} de controle apagadas; {} bairros dos Correios inseridos.",
                unificado,
                controle,
                inseridos
        );

        return new BairroBaseCorreiosResultadoDTO(
                unificado,
                controle,
                inseridos,
                "Base de bairros recomeçada do zero. Apagados: "
                        + unificado + " bairro(s) unificado(s) e "
                        + controle + " registro(s) de controle."
                        + " Inseridos: " + inseridos + " bairro(s) dos Correios."
        );
    }
}
