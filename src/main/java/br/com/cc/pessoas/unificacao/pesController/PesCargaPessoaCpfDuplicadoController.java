package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesService.PesCargaPessoaCpfDuplicadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pes-carga-pessoas-cpf-duplicado")
public class PesCargaPessoaCpfDuplicadoController {

    private final PesCargaPessoaCpfDuplicadoService service;

    public PesCargaPessoaCpfDuplicadoController(PesCargaPessoaCpfDuplicadoService service) {
        this.service = service;
    }

    @PostMapping("/processar/{pessoaId}")
    public ResponseEntity<String> processarPessoa(@PathVariable Long pessoaId) {
        service.processarPessoaUnica(pessoaId);
        return ResponseEntity.ok("Pessoa duplicada processada com sucesso.");
    }
}