package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.controller.base.BaseDescricaoController;
import br.com.cc.pessoas.entity.TipoPessoa;
import br.com.cc.pessoas.service.TipoPessoaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tipos-pessoas")
public class TipoPessoaController
        extends BaseDescricaoController<TipoPessoa> {

    public TipoPessoaController(TipoPessoaService service) {
        super(service);
    }
}
