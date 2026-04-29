package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.controle.ctrlEntity.PesCargaPessoasCtrlDto;
import br.com.cc.pessoas.unificacao.pesService.PesCargaPessoaCnpjUnicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pes-carga-pessoas-cnpj-unico")
public class PesCargaPessoaCnpjUnicoController {

    private final PesCargaPessoaCnpjUnicoService service;

    public PesCargaPessoaCnpjUnicoController(PesCargaPessoaCnpjUnicoService service) {
        this.service = service;
    }

    @PostMapping("/processar/{pessoaId}")
    public ResponseEntity<String> processarPessoa(@PathVariable Long pessoaId) {
        service.processarPessoaUnica(pessoaId);
        return ResponseEntity.ok("Pessoa jurídica processada com sucesso.");
    }

    @GetMapping("/status/{idControle}")
    public ResponseEntity<PesCargaPessoasCtrlDto> buscarStatus(@PathVariable Long idControle) {
        return ResponseEntity.ok(service.buscarStatus(idControle));
    }
}