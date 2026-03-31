package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesCidadeDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesCidadeFilter;
import br.com.cc.pessoas.unificacao.pesService.PesCidadeService;
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
@RequestMapping("/pes-cidades")
@RequiredArgsConstructor
public class PesCidadeController {

    @Autowired
    private PesCidadeService service;

    @GetMapping
    public Page<PesCidadeDTO> pesquisar(@ModelAttribute PesCidadeFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesCidadeDTO> listar(@ModelAttribute PesCidadeFilter filter) {
        return service.filtrar(filter);
    }
}