package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesEstadoDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesEstadoFilter;
import br.com.cc.pessoas.unificacao.pesService.PesEstadoService;
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
@RequestMapping("/pes-estados")
@RequiredArgsConstructor
public class PesEstadoController {

    @Autowired
    private PesEstadoService service;

    @GetMapping
    public Page<PesEstadoDTO> pesquisar(@ModelAttribute PesEstadoFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesEstadoDTO> listar(@ModelAttribute PesEstadoFilter filter) {
        return service.filtrar(filter);
    }
}