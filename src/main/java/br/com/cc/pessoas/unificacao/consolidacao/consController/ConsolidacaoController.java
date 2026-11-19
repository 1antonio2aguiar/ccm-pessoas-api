package br.com.cc.pessoas.unificacao.consolidacao.consController;

import br.com.cc.pessoas.unificacao.consolidacao.consDto.ConsolidacaoResumoDTO;
import br.com.cc.pessoas.unificacao.consolidacao.consDto.NivelConsolidacaoDTO;
import br.com.cc.pessoas.unificacao.consolidacao.consService.ConsolidacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Consolidação dos endereços unificados nas tabelas de DBO_UNI_PESSOAS
 * (CIDADES, DISTRITOS, BAIRROS, LOGRADOUROS, CEPS).
 *
 * {nivel}: CIDADES, DISTRITOS, BAIRROS, LOGRADOUROS ou CEPS.
 * Logradouros e CEPs têm mais de 1 milhão de linhas: levam alguns minutos.
 */
@Slf4j
@RestController
@RequestMapping("/consolidacao-enderecos")
@RequiredArgsConstructor
public class ConsolidacaoController {

    private final ConsolidacaoService service;

    @GetMapping("/niveis")
    public List<NivelConsolidacaoDTO> listarNiveis() {
        return service.listarNiveis();
    }

    @GetMapping("/{nivel}/resumo")
    public ConsolidacaoResumoDTO buscarResumo(
            @PathVariable String nivel) {

        return service.buscarResumo(nivel);
    }

    @PostMapping("/{nivel}/executar")
    public ResponseEntity<?> executar(
            @PathVariable String nivel) {

        try {
            return ResponseEntity.ok(service.executar(nivel));
        } catch (Exception e) {
            log.error("Erro ao consolidar o nível {}.", nivel, e);
            return erro("Não foi possível consolidar. ", e);
        }
    }

    @PostMapping("/executar-todos")
    public ResponseEntity<?> executarTodos() {

        try {
            return ResponseEntity.ok(service.executarTodos());
        } catch (Exception e) {
            log.error("Erro ao consolidar todos os níveis.", e);
            return erro("Não foi possível consolidar. ", e);
        }
    }

    private ResponseEntity<String> erro(String prefixo, Throwable e) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(prefixo + obterMensagemErro(e) + " Nenhuma alteração foi realizada.");
    }

    private String obterMensagemErro(Throwable erro) {

        if (erro == null) {
            return "Erro não identificado.";
        }

        Throwable causa = erro;

        while (causa.getCause() != null) {
            causa = causa.getCause();
        }

        if (causa.getMessage() == null || causa.getMessage().isBlank()) {
            return "Erro não identificado.";
        }

        return causa.getMessage().trim();
    }
}
