package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesService.PesCargaPessoaCnpjDuplicadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pes-carga-pessoas-cnpj-duplicado")
@RequiredArgsConstructor
public class PesCargaPessoaCnpjDuplicadoController {

    private final PesCargaPessoaCnpjDuplicadoService service;

    @PostMapping("/processar/{pessoaId}")
    public void processarPessoa(@PathVariable Long pessoaId) {
        service.processarPessoaUnica(pessoaId);
    }
}