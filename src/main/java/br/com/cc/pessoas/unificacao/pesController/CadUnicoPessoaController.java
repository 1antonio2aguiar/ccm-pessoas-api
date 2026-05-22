package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa.CadUnicoPessoaOrigemDTO;
import br.com.cc.pessoas.unificacao.pesService.CadUnicoPessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cad-unico-pessoas")
@RequiredArgsConstructor
public class CadUnicoPessoaController {

    private final CadUnicoPessoaService service;

    @GetMapping("/{pessoasCdUnico}/origens")
    public List<CadUnicoPessoaOrigemDTO> buscarOrigens(@PathVariable Long pessoasCdUnico) {
        return service.buscarOrigens(pessoasCdUnico);
    }
}