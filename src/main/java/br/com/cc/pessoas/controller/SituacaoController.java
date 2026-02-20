package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.controller.base.BaseDescricaoController;
import br.com.cc.pessoas.entity.Situacao;
import br.com.cc.pessoas.entity.TipoPessoa;
import br.com.cc.pessoas.service.SituacaoService;
import br.com.cc.pessoas.service.TipoPessoaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/situacoes")
public class SituacaoController
        extends BaseDescricaoController<Situacao> {

    public SituacaoController(SituacaoService service) {
        super(service);
    }
}
