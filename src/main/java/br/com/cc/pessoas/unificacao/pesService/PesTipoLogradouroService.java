package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesTipoLogradouroDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoLogradouroFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesTipoLogradouroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesTipoLogradouroService {
    @Autowired private PesTipoLogradouroRepository repository;
    public Page<PesTipoLogradouroDTO> filtrar(PesTipoLogradouroFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesTipoLogradouroDTO::fromEntity);
    }

    public List<PesTipoLogradouroDTO> filtrar(PesTipoLogradouroFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesTipoLogradouroDTO::fromEntity)
                .toList();
    }
}