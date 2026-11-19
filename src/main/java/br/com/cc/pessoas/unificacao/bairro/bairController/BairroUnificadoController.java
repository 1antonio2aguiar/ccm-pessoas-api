package br.com.cc.pessoas.unificacao.bairro.bairController;

import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroBaseCorreiosResumoDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroCandidatoDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroOrigemDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.OrigemBairroDTO;
import br.com.cc.pessoas.unificacao.bairro.bairFilter.BairroUnificadoFilter;
import br.com.cc.pessoas.unificacao.bairro.bairService.BairroBaseCorreiosService;
import br.com.cc.pessoas.unificacao.bairro.bairService.BairroUnificadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Unificação de bairros: grava em DBO_UNI_PESSOAS.BAIRROS_UNIFICADO
 * a cidade + distrito + bairro de cada sistema de origem.
 *
 * {origem}: PES, RH, SANE, SAU ou ESCO.
 * O bairro da origem é identificado por {cidade}/{distrito}/{bairro}.
 */
@Slf4j
@RestController
@RequestMapping("/bairros-unificado")
@RequiredArgsConstructor
public class BairroUnificadoController {

    private final BairroUnificadoService service;
    private final BairroBaseCorreiosService baseCorreiosService;

    /**********************************************************************
     * BASE DOS CORREIOS
     **********************************************************************/

    @GetMapping("/base-correios/resumo")
    public BairroBaseCorreiosResumoDTO buscarResumoBaseCorreios() {
        return baseCorreiosService.buscarResumo();
    }

    @PostMapping("/base-correios/gerar")
    public ResponseEntity<?> gerarBaseCorreios() {

        try {
            return ResponseEntity.ok(baseCorreiosService.gerar());
        } catch (Exception e) {
            log.error("Erro ao gerar a base de bairros dos Correios.", e);
            return erro("Não foi possível gerar a base de bairros. ", e);
        }
    }

    @PostMapping("/base-correios/zerar")
    public ResponseEntity<?> zerarBaseCorreios(
            @RequestParam String confirmacao) {

        try {
            return ResponseEntity.ok(baseCorreiosService.zerar(confirmacao));
        } catch (Exception e) {
            log.error("Erro ao recomeçar a unificação de bairros.", e);
            return erro("Não foi possível recomeçar do zero. ", e);
        }
    }

    /**********************************************************************
     * UNIFICAÇÃO POR ORIGEM
     **********************************************************************/

    @GetMapping("/origens")
    public List<OrigemBairroDTO> listarOrigens() {
        return service.listarOrigens();
    }

    @GetMapping("/{origem}/pendentes")
    public Page<BairroOrigemDTO> filtrarPendentes(
            @PathVariable String origem,
            @ModelAttribute BairroUnificadoFilter filter,
            Pageable pageable) {

        return service.filtrarPendentes(origem, filter, pageable);
    }

    @GetMapping("/{origem}/resumo")
    public BairroUnificadoResumoDTO buscarResumo(
            @PathVariable String origem) {

        return service.buscarResumo(origem);
    }

    @GetMapping("/{origem}/candidatos/{cidade}/{distrito}/{bairro}")
    public List<BairroCandidatoDTO> buscarCandidatos(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @PathVariable Long bairro,
            @RequestParam(required = false) String pesquisa,
            @RequestParam(defaultValue = "false") boolean todos) {

        return service.buscarCandidatos(origem, cidade, distrito, bairro, pesquisa, todos);
    }

    @PostMapping("/{origem}/vincular/{cidade}/{distrito}/{bairro}")
    public ResponseEntity<String> vincular(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @PathVariable Long bairro,
            @RequestParam Long idUnificado) {

        try {
            return ResponseEntity.ok(
                    service.vincular(origem, cidade, distrito, bairro, idUnificado)
            );
        } catch (Exception e) {
            log.error("Erro ao vincular o bairro {}/{}/{} da origem {}.",
                    cidade, distrito, bairro, origem, e);
            return erro("Não foi possível vincular o bairro. ", e);
        }
    }

    @PostMapping("/{origem}/vincular-unicos")
    public ResponseEntity<?> vincularUnicos(
            @PathVariable String origem) {

        try {
            return ResponseEntity.ok(service.vincularUnicos(origem));
        } catch (Exception e) {
            log.error("Erro ao vincular os bairros únicos da origem {}.", origem, e);
            return erro("Não foi possível executar a vinculação em lote. ", e);
        }
    }

    @PostMapping("/{origem}/criar/{cidade}/{distrito}/{bairro}")
    public ResponseEntity<String> criar(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @PathVariable Long bairro) {

        try {
            return ResponseEntity.ok(service.criar(origem, cidade, distrito, bairro));
        } catch (Exception e) {
            log.error("Erro ao criar o bairro {}/{}/{} da origem {}.",
                    cidade, distrito, bairro, origem, e);
            return erro("Não foi possível criar o bairro. ", e);
        }
    }

    @PostMapping("/{origem}/nao-vincular/{cidade}/{distrito}/{bairro}")
    public ResponseEntity<String> naoVincular(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @PathVariable Long bairro,
            @RequestParam(required = false) String motivo) {

        try {
            return ResponseEntity.ok(
                    service.naoVincular(origem, cidade, distrito, bairro, motivo)
            );
        } catch (Exception e) {
            log.error("Erro ao registrar não vincular do bairro {}/{}/{} da origem {}.",
                    cidade, distrito, bairro, origem, e);
            return erro("Não foi possível retirar o bairro da lista. ", e);
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
