package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesLogradouroDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesLogradouroFilter;
import br.com.cc.pessoas.unificacao.pesService.PesLogradouroService;
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
@RequestMapping("/pes-logradouros")
@RequiredArgsConstructor
public class PesLogradouroController {

    @Autowired
    private PesLogradouroService service;

    @GetMapping
    public Page<PesLogradouroDTO> pesquisar(@ModelAttribute PesLogradouroFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesLogradouroDTO> listar(@ModelAttribute PesLogradouroFilter filter) {
        return service.filtrar(filter);
    }
}