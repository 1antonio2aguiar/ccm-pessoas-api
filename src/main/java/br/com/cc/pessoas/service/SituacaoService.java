package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.base.DescricaoDTO;
import br.com.cc.pessoas.entity.Situacao;
import br.com.cc.pessoas.entity.TipoPessoa;
import br.com.cc.pessoas.repository.SituacaoRepository;
import br.com.cc.pessoas.service.base.BaseDescricaoService;
import org.springframework.stereotype.Service;

@Service
public class SituacaoService
        extends BaseDescricaoService<Situacao> {

    public SituacaoService(SituacaoRepository repository) {
        super(repository);
    }

    @Override
    protected DescricaoDTO toDTO(Situacao entity) {
        return new DescricaoDTO(
                entity.getId(),
                entity.getDescricao()
        );
    }

    @Override
    protected Situacao newEntity() {
        return new Situacao();
    }
}
