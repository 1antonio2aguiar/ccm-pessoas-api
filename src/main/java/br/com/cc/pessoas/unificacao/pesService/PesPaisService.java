package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesPaisDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesPaisFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesPaisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesPaisService {
    @Autowired private PesPaisRepository repository;
    public Page<PesPaisDTO> filtrar(PesPaisFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesPaisDTO::fromEntity);
    }

    public List<PesPaisDTO> filtrar(PesPaisFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesPaisDTO::fromEntity)
                .toList();
    }
}