package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesTipoPessoaDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoPessoaFilter;
import br.com.cc.pessoas.unificacao.pesService.PesTipoPessoaService;
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
@RequestMapping("/pes-tipos-pessoas")
@RequiredArgsConstructor
public class PesTipoPessoaController {

    @Autowired
    private PesTipoPessoaService service;

    @GetMapping
    public Page<PesTipoPessoaDTO> pesquisar(@ModelAttribute PesTipoPessoaFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesTipoPessoaDTO> listar(@ModelAttribute PesTipoPessoaFilter filter) {
        return service.filtrar(filter);
    }
}