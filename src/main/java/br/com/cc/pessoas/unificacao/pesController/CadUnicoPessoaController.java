package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesPaisDTO;
import br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa.CadUnicoPessoaCreateDTO;
import br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa.CadUnicoPessoaDTO;
import br.com.cc.pessoas.unificacao.pesFilter.CadUnicoPessoaFilter;
import br.com.cc.pessoas.unificacao.pesFilter.PesPaisFilter;
import br.com.cc.pessoas.unificacao.pesService.CadUnicoPessoaService;
import br.com.cc.pessoas.unificacao.pesService.PesPaisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pes-cad-unico-pessoas")
@RequiredArgsConstructor
public class CadUnicoPessoaController {

    @Autowired
    private CadUnicoPessoaService service;

    @GetMapping
    public Page<CadUnicoPessoaDTO> pesquisar(@ModelAttribute CadUnicoPessoaFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<CadUnicoPessoaDTO> listar(@ModelAttribute CadUnicoPessoaFilter filter) {
        return service.filtrar(filter);
    }

    @PostMapping
    public CadUnicoPessoaDTO salvar(@RequestBody CadUnicoPessoaCreateDTO dto) {
        return service.salvar(dto);
    }
}