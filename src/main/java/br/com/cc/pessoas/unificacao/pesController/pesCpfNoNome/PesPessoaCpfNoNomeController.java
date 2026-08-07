package br.com.cc.pessoas.unificacao.pesController.pesCpfNoNome;

import br.com.cc.pessoas.unificacao.pessoa.pesDto.pesCpfNoNome.PesPessoaCpfNoNomeDTO;
import br.com.cc.pessoas.unificacao.pesService.pesCpfNoNome.PesPessoaCpfNoNomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pes-pessoas/cpf-no-nome")
@RequiredArgsConstructor
public class PesPessoaCpfNoNomeController {

    private final PesPessoaCpfNoNomeService service;

    /*@GetMapping
    public Page<PesPessoaCpfNoNomeDTO> listar(
            @RequestParam(required = false) String nome,
            Pageable pageable) {

        return service.listarCpfNoNome(nome, pageable);
    }*/
}