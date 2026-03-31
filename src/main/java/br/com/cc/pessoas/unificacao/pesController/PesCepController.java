package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesCepDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesCepFilter;
import br.com.cc.pessoas.unificacao.pesService.PesCepService;
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
@RequestMapping("/pes-ceps")
@RequiredArgsConstructor
public class PesCepController {

    @Autowired
    private PesCepService service;

    @GetMapping
    public Page<PesCepDTO> pesquisar(@ModelAttribute PesCepFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesCepDTO> listar(@ModelAttribute PesCepFilter filter) {
        return service.filtrar(filter);
    }
}