package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.controller.base.BaseDescricaoController;
import br.com.cc.pessoas.entity.TituloPatente;
import br.com.cc.pessoas.service.TituloPatenteService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/titulo-patentes")
public class TituloPatenteController extends BaseDescricaoController<TituloPatente> {

    public TituloPatenteController(TituloPatenteService service) {
        super(service);
    }
}
