package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.controller.base.BaseDescricaoController;
import br.com.cc.pessoas.dto.base.DescricaoDTO;
import br.com.cc.pessoas.entity.TituloPatente;
import br.com.cc.pessoas.service.TituloPatenteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/titulo-patentes")
public class TituloPatenteController extends BaseDescricaoController<TituloPatente> {

    private final TituloPatenteService tituloPatenteService;

    public TituloPatenteController(TituloPatenteService service) {
        super(service);
        this.tituloPatenteService = service;
    }

    @GetMapping("/listar")
    public List<DescricaoDTO> listarPorDescricao(@RequestParam(required = false) String descricao) {
        return tituloPatenteService.listarPorDescricao(descricao);
    }
}