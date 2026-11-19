package br.com.cc.pessoas.unificacao.distrito.distController;

import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoBaseCorreiosResumoDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoCandidatoDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoOrigemDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.OrigemDistritoDTO;
import br.com.cc.pessoas.unificacao.distrito.distFilter.DistritoUnificadoFilter;
import br.com.cc.pessoas.unificacao.distrito.distService.DistritoBaseCorreiosService;
import br.com.cc.pessoas.unificacao.distrito.distService.DistritoUnificadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Unificação de distritos: grava em DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO
 * a cidade + distrito de cada sistema de origem.
 *
 * {origem}: PES, RH, SANE, SAU ou ESCO.
 * O distrito da origem é identificado por {cidade}/{distrito}.
 */
@Slf4j
@RestController
@RequestMapping("/distritos-unificado")
@RequiredArgsConstructor
public class DistritoUnificadoController {

    private final DistritoUnificadoService service;
    private final DistritoBaseCorreiosService baseCorreiosService;

    /**********************************************************************
     * BASE DOS CORREIOS
     **********************************************************************/

    @GetMapping("/base-correios/resumo")
    public DistritoBaseCorreiosResumoDTO buscarResumoBaseCorreios() {
        return baseCorreiosService.buscarResumo();
    }

    @PostMapping("/base-correios/gerar")
    public ResponseEntity<?> gerarBaseCorreios() {

        try {
            return ResponseEntity.ok(baseCorreiosService.gerar());
        } catch (Exception e) {
            log.error("Erro ao gerar a base de distritos dos Correios.", e);
            return erro("Não foi possível gerar a base de distritos. ", e);
        }
    }

    @PostMapping("/base-correios/zerar")
    public ResponseEntity<?> zerarBaseCorreios(
            @RequestParam String confirmacao) {

        try {
            return ResponseEntity.ok(baseCorreiosService.zerar(confirmacao));
        } catch (Exception e) {
            log.error("Erro ao recomeçar a unificação de distritos.", e);
            return erro("Não foi possível recomeçar do zero. ", e);
        }
    }

    /**********************************************************************
     * UNIFICAÇÃO POR ORIGEM
     **********************************************************************/

    @GetMapping("/origens")
    public List<OrigemDistritoDTO> listarOrigens() {
        return service.listarOrigens();
    }

    @GetMapping("/{origem}/pendentes")
    public Page<DistritoOrigemDTO> filtrarPendentes(
            @PathVariable String origem,
            @ModelAttribute DistritoUnificadoFilter filter,
            Pageable pageable) {

        return service.filtrarPendentes(origem, filter, pageable);
    }

    @GetMapping("/{origem}/resumo")
    public DistritoUnificadoResumoDTO buscarResumo(
            @PathVariable String origem) {

        return service.buscarResumo(origem);
    }

    @GetMapping("/{origem}/candidatos/{cidade}/{distrito}")
    public List<DistritoCandidatoDTO> buscarCandidatos(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @RequestParam(required = false) String pesquisa,
            @RequestParam(defaultValue = "false") boolean todos) {

        return service.buscarCandidatos(origem, cidade, distrito, pesquisa, todos);
    }

    @PostMapping("/{origem}/vincular/{cidade}/{distrito}")
    public ResponseEntity<String> vincular(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @RequestParam Long idUnificado) {

        try {
            return ResponseEntity.ok(
                    service.vincular(origem, cidade, distrito, idUnificado)
            );
        } catch (Exception e) {
            log.error("Erro ao vincular o distrito {}/{} da origem {}.", cidade, distrito, origem, e);
            return erro("Não foi possível vincular o distrito. ", e);
        }
    }

    @PostMapping("/{origem}/vincular-unicos")
    public ResponseEntity<?> vincularUnicos(
            @PathVariable String origem) {

        try {
            return ResponseEntity.ok(service.vincularUnicos(origem));
        } catch (Exception e) {
            log.error("Erro ao vincular os distritos únicos da origem {}.", origem, e);
            return erro("Não foi possível executar a vinculação em lote. ", e);
        }
    }

    @PostMapping("/{origem}/criar/{cidade}/{distrito}")
    public ResponseEntity<String> criar(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito) {

        try {
            return ResponseEntity.ok(service.criar(origem, cidade, distrito));
        } catch (Exception e) {
            log.error("Erro ao criar o distrito {}/{} da origem {}.", cidade, distrito, origem, e);
            return erro("Não foi possível criar o distrito. ", e);
        }
    }

    @PostMapping("/{origem}/nao-vincular/{cidade}/{distrito}")
    public ResponseEntity<String> naoVincular(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @RequestParam(required = false) String motivo) {

        try {
            return ResponseEntity.ok(
                    service.naoVincular(origem, cidade, distrito, motivo)
            );
        } catch (Exception e) {
            log.error("Erro ao registrar não vincular do distrito {}/{} da origem {}.",
                    cidade, distrito, origem, e);
            return erro("Não foi possível retirar o distrito da lista. ", e);
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
