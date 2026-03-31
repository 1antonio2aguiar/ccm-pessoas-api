package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesTipoLogradouroDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoLogradouroFilter;
import br.com.cc.pessoas.unificacao.pesService.PesTipoLogradouroService;
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
@RequestMapping("/pes-tipos-logradouros")
@RequiredArgsConstructor
public class PesTipoLogradouroController {

    @Autowired
    private PesTipoLogradouroService service;

    @GetMapping
    public Page<PesTipoLogradouroDTO> pesquisar(@ModelAttribute PesTipoLogradouroFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesTipoLogradouroDTO> listar(@ModelAttribute PesTipoLogradouroFilter filter) {
        return service.filtrar(filter);
    }
}