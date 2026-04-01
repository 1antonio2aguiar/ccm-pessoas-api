package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesPessoaDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesPessoaFilter;
import br.com.cc.pessoas.unificacao.pesService.PesPessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pes-pessoas")
@RequiredArgsConstructor
public class PesPessoaController {

    @Autowired
    private PesPessoaService service;

    @GetMapping
    public Page<PesPessoaDTO> pesquisar(@ModelAttribute PesPessoaFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesPessoaDTO> listar(@ModelAttribute PesPessoaFilter filter) {
        return service.filtrar(filter);
    }
}