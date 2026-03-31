package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesPaisDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesPaisFilter;
import br.com.cc.pessoas.unificacao.pesService.PesPaisService;
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
@RequestMapping("/pes-paises")
@RequiredArgsConstructor
public class PesPaisController {

    @Autowired
    private PesPaisService service;

    @GetMapping
    public Page<PesPaisDTO> pesquisar(@ModelAttribute PesPaisFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesPaisDTO> listar(@ModelAttribute PesPaisFilter filter) {
        return service.filtrar(filter);
    }
}