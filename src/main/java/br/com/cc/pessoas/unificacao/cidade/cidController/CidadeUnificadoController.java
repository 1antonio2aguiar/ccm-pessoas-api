package br.com.cc.pessoas.unificacao.cidade.cidController;

import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeBaseCorreiosResumoDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeCandidatoDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeOrigemDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.OrigemCidadeDTO;
import br.com.cc.pessoas.unificacao.cidade.cidFilter.CidadeUnificadoFilter;
import br.com.cc.pessoas.unificacao.cidade.cidService.CidadeBaseCorreiosService;
import br.com.cc.pessoas.unificacao.cidade.cidService.CidadeUnificadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Unificação de cidades: grava em DBO_UNI_PESSOAS.CIDADES_UNIFICADO
 * o código de cada sistema de origem.
 *
 * {origem}: PES, RH, SANE, SAU ou ESCO.
 */
@Slf4j
@RestController
@RequestMapping("/cidades-unificado")
@RequiredArgsConstructor
public class CidadeUnificadoController {

    private final CidadeUnificadoService service;
    private final CidadeBaseCorreiosService baseCorreiosService;

    /**********************************************************************
     * BASE DOS CORREIOS
     **********************************************************************/

    @GetMapping("/base-correios/resumo")
    public CidadeBaseCorreiosResumoDTO buscarResumoBaseCorreios() {
        return baseCorreiosService.buscarResumo();
    }

    /*
     * Incremental: só acrescenta as cidades que faltam.
     */
    @PostMapping("/base-correios/gerar")
    public ResponseEntity<?> gerarBaseCorreios() {

        try {
            return ResponseEntity.ok(
                    baseCorreiosService.gerar()
            );
        } catch (Exception e) {
            log.error("Erro ao gerar a base dos Correios.", e);
            return erro(
                    "Não foi possível gerar a base dos Correios. ",
                    e
            );
        }
    }

    /*
     * Apaga CIDADES_UNIFICADO e CIDADES_UNIFICADO_CTRL
     * e gera a base novamente.
     *
     * confirmacao deve ser "ZERAR".
     */
    @PostMapping("/base-correios/zerar")
    public ResponseEntity<?> zerarBaseCorreios(
            @RequestParam String confirmacao,
            @RequestParam(defaultValue = "false") boolean apagarFicticias) {

        try {
            return ResponseEntity.ok(
                    baseCorreiosService.zerar(
                            confirmacao,
                            apagarFicticias
                    )
            );
        } catch (Exception e) {
            log.error("Erro ao recomeçar a unificação de cidades.", e);
            return erro(
                    "Não foi possível recomeçar do zero. ",
                    e
            );
        }
    }

    /**********************************************************************
     * UNIFICAÇÃO POR ORIGEM
     **********************************************************************/

    @GetMapping("/origens")
    public List<OrigemCidadeDTO> listarOrigens() {
        return service.listarOrigens();
    }

    @GetMapping("/{origem}/pendentes")
    public Page<CidadeOrigemDTO> filtrarPendentes(
            @PathVariable String origem,
            @ModelAttribute CidadeUnificadoFilter filter,
            Pageable pageable) {

        return service.filtrarPendentes(
                origem,
                filter,
                pageable
        );
    }

    @GetMapping("/{origem}/resumo")
    public CidadeUnificadoResumoDTO buscarResumo(
            @PathVariable String origem) {

        return service.buscarResumo(origem);
    }

    @GetMapping("/{origem}/candidatos/{cidade}")
    public List<CidadeCandidatoDTO> buscarCandidatos(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @RequestParam(required = false) String pesquisa,
            @RequestParam(required = false) String uf) {

        return service.buscarCandidatos(
                origem,
                cidade,
                pesquisa,
                uf
        );
    }

    @PostMapping("/{origem}/vincular/{cidade}")
    public ResponseEntity<String> vincular(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @RequestParam Long idUnificado) {

        try {
            return ResponseEntity.ok(
                    service.vincular(
                            origem,
                            cidade,
                            idUnificado
                    )
            );
        } catch (Exception e) {
            log.error(
                    "Erro ao vincular a cidade {} da origem {}.",
                    cidade,
                    origem,
                    e
            );
            return erro(
                    "Não foi possível vincular a cidade. ",
                    e
            );
        }
    }

    @PostMapping("/{origem}/vincular-unicas")
    public ResponseEntity<?> vincularUnicas(
            @PathVariable String origem) {

        try {
            return ResponseEntity.ok(
                    service.vincularUnicas(origem)
            );
        } catch (Exception e) {
            log.error(
                    "Erro ao vincular as cidades únicas da origem {}.",
                    origem,
                    e
            );
            return erro(
                    "Não foi possível executar a vinculação em lote. ",
                    e
            );
        }
    }

    @PostMapping("/{origem}/criar/{cidade}")
    public ResponseEntity<String> criar(
            @PathVariable String origem,
            @PathVariable Long cidade) {

        try {
            return ResponseEntity.ok(
                    service.criar(
                            origem,
                            cidade
                    )
            );
        } catch (Exception e) {
            log.error(
                    "Erro ao criar a cidade {} da origem {}.",
                    cidade,
                    origem,
                    e
            );
            return erro(
                    "Não foi possível criar a cidade. ",
                    e
            );
        }
    }

    @PostMapping("/{origem}/nao-vincular/{cidade}")
    public ResponseEntity<String> naoVincular(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @RequestParam(required = false) String motivo) {

        try {
            return ResponseEntity.ok(
                    service.naoVincular(
                            origem,
                            cidade,
                            motivo
                    )
            );
        } catch (Exception e) {
            log.error(
                    "Erro ao registrar não vincular da cidade {} da origem {}.",
                    cidade,
                    origem,
                    e
            );
            return erro(
                    "Não foi possível retirar a cidade da lista. ",
                    e
            );
        }
    }

    private ResponseEntity<String> erro(
            String prefixo,
            Throwable e) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(
                        prefixo
                                + obterMensagemErro(e)
                                + " Nenhuma alteração foi realizada."
                );
    }

    private String obterMensagemErro(
            Throwable erro) {

        if (erro == null) {
            return "Erro não identificado.";
        }

        Throwable causa = erro;

        while (causa.getCause() != null) {
            causa = causa.getCause();
        }

        if (causa.getMessage() == null
                || causa.getMessage().isBlank()) {
            return "Erro não identificado.";
        }

        return causa.getMessage().trim();
    }
}
