package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.base.DescricaoDTO;
import br.com.cc.pessoas.entity.TituloPatente;
import br.com.cc.pessoas.repository.TituloPatenteRepository;
import br.com.cc.pessoas.service.base.BaseDescricaoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TituloPatenteService extends BaseDescricaoService<TituloPatente> {

    private final TituloPatenteRepository tituloPatenteRepository;

    public TituloPatenteService(TituloPatenteRepository repository) {
        super(repository);
        this.tituloPatenteRepository = repository;
    }

    public List<DescricaoDTO> listarPorDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return listar(); // seu método padrão do BaseDescricaoService
        }

        return tituloPatenteRepository.findByDescricaoContainingIgnoreCase(descricao)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    protected DescricaoDTO toDTO(TituloPatente entity) {
        return new DescricaoDTO(entity.getId(), entity.getDescricao());
    }

    @Override
    protected TituloPatente newEntity() {
        return new TituloPatente();
    }
}