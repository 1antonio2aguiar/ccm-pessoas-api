package br.com.cc.pessoas.unificacao.cargafinal.cargController;

import br.com.cc.pessoas.unificacao.cargafinal.cargDto.CargaFinalResumoDTO;
import br.com.cc.pessoas.unificacao.cargafinal.cargDto.NivelCargaFinalDTO;
import br.com.cc.pessoas.unificacao.cargafinal.cargService.CargaFinalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Carga final dos endereços (fase 3): DBO_UNI_PESSOAS -> DBO_CC_ENDERECOS e
 * DBO_CCM_PESSOAS, gerando o CODIGO_CCM.
 *
 * {nivel}: CIDADES, DISTRITOS, BAIRROS, LOGRADOUROS ou CEPS.
 */
@Slf4j
@RestController
@RequestMapping("/carga-final-enderecos")
@RequiredArgsConstructor
public class CargaFinalController {

    private final CargaFinalService service;

    @GetMapping("/niveis")
    public List<NivelCargaFinalDTO> listarNiveis() {
        return service.listarNiveis();
    }

    @GetMapping("/{nivel}/resumo")
    public CargaFinalResumoDTO buscarResumo(
            @PathVariable String nivel) {

        return service.buscarResumo(nivel);
    }

    @PostMapping("/{nivel}/executar")
    public ResponseEntity<?> executar(
            @PathVariable String nivel) {

        try {
            return ResponseEntity.ok(service.executar(nivel));
        } catch (Exception e) {
            log.error("Erro na carga final do nível {}.", nivel, e);
            return erro("Não foi possível fazer a carga final. ", e);
        }
    }

    @PostMapping("/executar-todos")
    public ResponseEntity<?> executarTodos() {

        try {
            return ResponseEntity.ok(service.executarTodos());
        } catch (Exception e) {
            log.error("Erro na carga final de todos os níveis.", e);
            return erro("Não foi possível fazer a carga final. ", e);
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
