package br.com.cc.pessoas.unificacao.cidade.cidService;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeBaseCorreiosResultadoDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeBaseCorreiosResumoDTO;
import br.com.cc.pessoas.unificacao.cidade.cidRepository.CidadeBaseCorreiosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Base dos Correios da unificação de cidades.
 *
 * - gerar: incremental, só acrescenta as cidades dos Correios
 *   que ainda não estão em CIDADES_UNIFICADO;
 * - zerar: apaga CIDADES_UNIFICADO e CIDADES_UNIFICADO_CTRL
 *   e gera a base novamente (fase de testes).
 *
 * O zerar pode ser desligado com:
 *   unificacao.cidades.permitir-zerar=false
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CidadeBaseCorreiosService {

    public static final String TEXTO_CONFIRMACAO = "ZERAR";

    private final CidadeBaseCorreiosRepository repository;

    @Value("${unificacao.cidades.permitir-zerar:true}")
    private boolean permitirZerar;

    @Transactional(readOnly = true)
    public CidadeBaseCorreiosResumoDTO buscarResumo() {
        return new CidadeBaseCorreiosResumoDTO(
                repository.contarCorreios(),
                repository.contarCorreiosFicticias(),
                repository.contarUnificado(),
                repository.contarFaltandoNaBase(),
                permitirZerar
        );
    }

    @Transactional
    public CidadeBaseCorreiosResultadoDTO gerar() {

        long inseridas = repository.inserirFaltantes();

        return new CidadeBaseCorreiosResultadoDTO(
                0L,
                0L,
                0L,
                inseridas,
                inseridas == 0
                        ? "A base já estava completa. Nenhuma cidade inserida."
                        : inseridas + " cidade(s) dos Correios inserida(s) na base."
        );
    }

    /*
     * Tudo em uma única transação: se qualquer passo falhar,
     * nada é apagado.
     */
    @Transactional
    public CidadeBaseCorreiosResultadoDTO zerar(
            String confirmacao,
            boolean apagarFicticias) {

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

        long ficticias = apagarFicticias
                ? repository.apagarFicticiasSemUso()
                : 0L;

        long inseridas = repository.inserirFaltantes();

        log.warn(
                "Unificação de cidades zerada: {} linhas de CIDADES_UNIFICADO, "
                        + "{} de controle e {} cidades fictícias apagadas; "
                        + "{} cidades dos Correios inseridas.",
                unificado,
                controle,
                ficticias,
                inseridas
        );

        return new CidadeBaseCorreiosResultadoDTO(
                unificado,
                controle,
                ficticias,
                inseridas,
                "Base recomeçada do zero. Apagadas: "
                        + unificado + " cidade(s) unificada(s), "
                        + controle + " registro(s) de controle"
                        + (apagarFicticias
                                ? ", " + ficticias + " cidade(s) fictícia(s) sem uso"
                                : "")
                        + ". Inseridas: " + inseridas + " cidade(s) dos Correios."
        );
    }
}
