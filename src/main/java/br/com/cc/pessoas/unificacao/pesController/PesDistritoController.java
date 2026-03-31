package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesDistritoDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesDistritoFilter;
import br.com.cc.pessoas.unificacao.pesService.PesDistritoService;
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
@RequestMapping("/pes-distritos")
@RequiredArgsConstructor
public class PesDistritoController {

    @Autowired
    private PesDistritoService service;

    @GetMapping
    public Page<PesDistritoDTO> pesquisar(@ModelAttribute PesDistritoFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesDistritoDTO> listar(@ModelAttribute PesDistritoFilter filter) {
        return service.filtrar(filter);
    }
}