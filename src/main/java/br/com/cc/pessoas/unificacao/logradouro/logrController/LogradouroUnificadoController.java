package br.com.cc.pessoas.unificacao.logradouro.logrController;

import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroBaseCorreiosResumoDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroCandidatoDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroCepDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroOrigemDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.OrigemLogradouroDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrFilter.LogradouroUnificadoFilter;
import br.com.cc.pessoas.unificacao.logradouro.logrService.LogradouroBaseCorreiosService;
import br.com.cc.pessoas.unificacao.logradouro.logrService.LogradouroUnificadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Unificação de logradouros: grava em DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO
 * a cidade + distrito + logradouro de cada sistema de origem.
 *
 * {origem}: PES, RH, SANE, SAU ou ESCO.
 *
 * Base dos Correios: 1,3 milhão de linhas; gerar e zerar levam minutos.
 * O logradouro da origem é identificado por {cidade}/{distrito}/{logradouro}.
 */
@Slf4j
@RestController
@RequestMapping("/logradouros-unificado")
@RequiredArgsConstructor
public class LogradouroUnificadoController {

    private final LogradouroUnificadoService service;
    private final LogradouroBaseCorreiosService baseCorreiosService;

    /**********************************************************************
     * BASE DOS CORREIOS
     **********************************************************************/

    @GetMapping("/base-correios/resumo")
    public LogradouroBaseCorreiosResumoDTO buscarResumoBaseCorreios() {
        return baseCorreiosService.buscarResumo();
    }

    @PostMapping("/base-correios/gerar")
    public ResponseEntity<?> gerarBaseCorreios() {

        try {
            return ResponseEntity.ok(baseCorreiosService.gerar());
        } catch (Exception e) {
            log.error("Erro ao gerar a base de logradouros dos Correios.", e);
            return erro("Não foi possível gerar a base de logradouros. ", e);
        }
    }

    @PostMapping("/base-correios/zerar")
    public ResponseEntity<?> zerarBaseCorreios(
            @RequestParam String confirmacao) {

        try {
            return ResponseEntity.ok(baseCorreiosService.zerar(confirmacao));
        } catch (Exception e) {
            log.error("Erro ao recomeçar a unificação de logradouros.", e);
            return erro("Não foi possível recomeçar do zero. ", e);
        }
    }

    /**********************************************************************
     * UNIFICAÇÃO POR ORIGEM
     **********************************************************************/

    @GetMapping("/origens")
    public List<OrigemLogradouroDTO> listarOrigens() {
        return service.listarOrigens();
    }

    @GetMapping("/{origem}/pendentes")
    public Page<LogradouroOrigemDTO> filtrarPendentes(
            @PathVariable String origem,
            @ModelAttribute LogradouroUnificadoFilter filter,
            Pageable pageable) {

        return service.filtrarPendentes(origem, filter, pageable);
    }

    @GetMapping("/{origem}/resumo")
    public LogradouroUnificadoResumoDTO buscarResumo(
            @PathVariable String origem) {

        return service.buscarResumo(origem);
    }

    @GetMapping("/{origem}/candidatos/{cidade}/{distrito}/{logradouro}")
    public List<LogradouroCandidatoDTO> buscarCandidatos(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @PathVariable Long logradouro,
            @RequestParam(required = false) String pesquisa,
            @RequestParam(defaultValue = "false") boolean todos) {

        return service.buscarCandidatos(origem, cidade, distrito, logradouro, pesquisa, todos);
    }

    /*
     * CEPs válidos (tabela de CEPs dos Correios) para criar o logradouro.
     * pesquisa: começo do CEP ou parte do nome de um logradouro vizinho.
     */
    @GetMapping("/{origem}/ceps/{cidade}/{distrito}/{logradouro}")
    public List<LogradouroCepDTO> buscarCeps(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @PathVariable Long logradouro,
            @RequestParam(required = false) String pesquisa) {

        return service.buscarCeps(origem, cidade, distrito, logradouro, pesquisa);
    }

    @PostMapping("/{origem}/vincular/{cidade}/{distrito}/{logradouro}")
    public ResponseEntity<String> vincular(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @PathVariable Long logradouro,
            @RequestParam Long idUnificado) {

        try {
            return ResponseEntity.ok(
                    service.vincular(origem, cidade, distrito, logradouro, idUnificado)
            );
        } catch (Exception e) {
            log.error("Erro ao vincular o logradouro {}/{}/{} da origem {}.",
                    cidade, distrito, logradouro, origem, e);
            return erro("Não foi possível vincular o logradouro. ", e);
        }
    }

    @PostMapping("/{origem}/vincular-unicos")
    public ResponseEntity<?> vincularUnicos(
            @PathVariable String origem) {

        try {
            return ResponseEntity.ok(service.vincularUnicos(origem));
        } catch (Exception e) {
            log.error("Erro ao vincular os logradouros únicos da origem {}.", origem, e);
            return erro("Não foi possível executar a vinculação em lote. ", e);
        }
    }

    @PostMapping("/{origem}/criar/{cidade}/{distrito}/{logradouro}")
    public ResponseEntity<String> criar(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @PathVariable Long logradouro,
            @RequestParam(required = false) Long cep) {

        try {
            return ResponseEntity.ok(service.criar(origem, cidade, distrito, logradouro, cep));
        } catch (Exception e) {
            log.error("Erro ao criar o logradouro {}/{}/{} da origem {}.",
                    cidade, distrito, logradouro, origem, e);
            return erro("Não foi possível criar o logradouro. ", e);
        }
    }

    @PostMapping("/{origem}/nao-vincular/{cidade}/{distrito}/{logradouro}")
    public ResponseEntity<String> naoVincular(
            @PathVariable String origem,
            @PathVariable Long cidade,
            @PathVariable Long distrito,
            @PathVariable Long logradouro,
            @RequestParam(required = false) String motivo) {

        try {
            return ResponseEntity.ok(
                    service.naoVincular(origem, cidade, distrito, logradouro, motivo)
            );
        } catch (Exception e) {
            log.error("Erro ao registrar não vincular do logradouro {}/{}/{} da origem {}.",
                    cidade, distrito, logradouro, origem, e);
            return erro("Não foi possível retirar o logradouro da lista. ", e);
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
