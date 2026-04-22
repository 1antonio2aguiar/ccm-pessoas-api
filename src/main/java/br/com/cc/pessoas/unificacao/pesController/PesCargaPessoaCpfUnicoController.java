package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.controle.ctrlEntity.PesCargaPessoasCtrlDto;
import br.com.cc.pessoas.unificacao.pesService.PesCargaPessoaCpfUnicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pes-carga-pessoas-cpf-unico")
public class PesCargaPessoaCpfUnicoController {

    private final PesCargaPessoaCpfUnicoService service;

    public PesCargaPessoaCpfUnicoController(PesCargaPessoaCpfUnicoService service) {
        this.service = service;
    }

    @PostMapping("/processar/{pessoaId}")
    public ResponseEntity<String> processarPessoa(@PathVariable Long pessoaId) {
        service.processarPessoaUnica(pessoaId);
        return ResponseEntity.ok("Pessoa processada com sucesso.");
    }

    @GetMapping("/status/{idControle}")
    public ResponseEntity<PesCargaPessoasCtrlDto> buscarStatus(@PathVariable Long idControle) {
        return ResponseEntity.ok(service.buscarStatus(idControle));
    }
}