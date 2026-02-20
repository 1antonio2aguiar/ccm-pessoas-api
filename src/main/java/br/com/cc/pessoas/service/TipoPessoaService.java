package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.base.DescricaoDTO;
import br.com.cc.pessoas.entity.TipoPessoa;
import br.com.cc.pessoas.repository.TipoPessoaRepository;
import br.com.cc.pessoas.service.base.BaseDescricaoService;
import org.springframework.stereotype.Service;

@Service
public class TipoPessoaService
        extends BaseDescricaoService<TipoPessoa> {

    public TipoPessoaService(TipoPessoaRepository repository) {
        super(repository);
    }

    @Override
    protected DescricaoDTO toDTO(TipoPessoa entity) {
        return new DescricaoDTO(
                entity.getId(),
                entity.getDescricao()
        );
    }

    @Override
    protected TipoPessoa newEntity() {
        return new TipoPessoa();
    }
}
