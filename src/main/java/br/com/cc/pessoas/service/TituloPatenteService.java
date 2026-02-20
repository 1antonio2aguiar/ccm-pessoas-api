package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.base.DescricaoDTO;
import br.com.cc.pessoas.entity.TituloPatente;
import br.com.cc.pessoas.repository.TituloPatenteRepository;
import br.com.cc.pessoas.service.base.BaseDescricaoService;
import org.springframework.stereotype.Service;

@Service
public class TituloPatenteService extends BaseDescricaoService<TituloPatente> {

    public TituloPatenteService(TituloPatenteRepository repository) {
        super(repository);
    }

    @Override
    protected DescricaoDTO toDTO(TituloPatente entity) {
        return new DescricaoDTO(
                entity.getId(),
                entity.getDescricao()
        );
    }

    @Override
    protected TituloPatente newEntity() {
        return new TituloPatente();
    }
}
