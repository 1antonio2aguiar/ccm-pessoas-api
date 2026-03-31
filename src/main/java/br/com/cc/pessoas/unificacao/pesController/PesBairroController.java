package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesBairroDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesBairroFilter;
import br.com.cc.pessoas.unificacao.pesService.PesBairroService;
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
@RequestMapping("/pes-bairros")
@RequiredArgsConstructor
public class PesBairroController {

    @Autowired
    private PesBairroService service;

    @GetMapping
    public Page<PesBairroDTO> pesquisar(@ModelAttribute PesBairroFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesBairroDTO> listar(@ModelAttribute PesBairroFilter filter) {
        return service.filtrar(filter);
    }
}