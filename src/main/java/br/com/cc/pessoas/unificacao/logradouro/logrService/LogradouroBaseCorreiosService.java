package br.com.cc.pessoas.unificacao.logradouro.logrService;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroBaseCorreiosResultadoDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroBaseCorreiosResumoDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrRepository.LogradouroBaseCorreiosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Base dos Correios da unificação de logradouros.
 *
 * - gerar: incremental, só acrescenta os logradouros que faltam e calcula
 *   as chaves de comparação que estiverem vazias;
 * - zerar: desfaz todos os vínculos e o controle, sem apagar e reinserir os
 *   logradouros dos Correios (fase de testes). O custo é proporcional ao que
 *   estava vinculado, não ao tamanho da base.
 *
 * Usa a mesma chave de cidades para desligar o zerar:
 *   unificacao.cidades.permitir-zerar=false
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogradouroBaseCorreiosService {

    public static final String TEXTO_CONFIRMACAO = "ZERAR";

    private final LogradouroBaseCorreiosRepository repository;

    @Value("${unificacao.cidades.permitir-zerar:true}")
    private boolean permitirZerar;

    @Transactional(readOnly = true)
    public LogradouroBaseCorreiosResumoDTO buscarResumo() {
        return new LogradouroBaseCorreiosResumoDTO(
                repository.contarCorreios(),
                repository.contarCorreiosFicticios(),
                repository.contarUnificado(),
                repository.contarFaltandoNaBase(),
                repository.contarSemChave(),
                permitirZerar
        );
    }

    @Transactional
    public LogradouroBaseCorreiosResultadoDTO gerar() {

        long inseridos = repository.inserirFaltantes();
        long chaves = repository.calcularChavesVazias();

        String mensagem;

        if (inseridos == 0 && chaves == 0) {
            mensagem = "A base já estava completa. Nenhum logradouro inserido.";
        } else {
            mensagem = inseridos + " logradouro(s) dos Correios inserido(s) na base";

            if (chaves > 0) {
                mensagem += " e " + chaves + " chave(s) de comparação calculada(s)";
            }

            mensagem += ".";
        }

        return new LogradouroBaseCorreiosResultadoDTO(0L, 0L, inseridos, chaves, mensagem);
    }

    /*
     * Tudo em uma única transação: se qualquer passo falhar, nada é apagado.
     */
    @Transactional
    public LogradouroBaseCorreiosResultadoDTO zerar(String confirmacao) {

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
        long semCodigo = repository.apagarSemCodigoCorreios();
        long repetidos = repository.apagarRepetidos();
        long vinculos = repository.zerarVinculos();
        long inseridos = repository.inserirFaltantes();
        long chaves = repository.calcularChavesVazias();

        log.warn(
                "Unificação de logradouros zerada: {} vínculos zerados, {} linhas sem código "
                        + "dos Correios e {} repetidas apagadas, {} de controle apagadas; "
                        + "{} logradouros inseridos, {} chaves calculadas.",
                vinculos,
                semCodigo,
                repetidos,
                controle,
                inseridos,
                chaves
        );

        return new LogradouroBaseCorreiosResultadoDTO(
                semCodigo + repetidos,
                controle,
                inseridos,
                chaves,
                "Base de logradouros recomeçada do zero. "
                        + vinculos + " logradouro(s) desvinculado(s); apagados "
                        + semCodigo + " sem código dos Correios, "
                        + repetidos + " repetido(s) e "
                        + controle + " registro(s) de controle."
                        + " Inseridos: " + inseridos + " logradouro(s) dos Correios."
        );
    }
}
