package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesTipoDocumentoDTO;
import br.com.cc.pessoas.unificacao.pesDto.PesTipoPessoaDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoDocumentoFilter;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoPessoaFilter;
import br.com.cc.pessoas.unificacao.pesService.PesTipoDocumentoService;
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
@RequestMapping("/pes-tipos-documentos")
@RequiredArgsConstructor
public class PesTipoDocumentoController {

    @Autowired
    private PesTipoDocumentoService service;

    @GetMapping
    public Page<PesTipoDocumentoDTO> pesquisar(@ModelAttribute PesTipoDocumentoFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesTipoDocumentoDTO> listar(@ModelAttribute PesTipoDocumentoFilter filter) {
        return service.filtrar(filter);
    }
}