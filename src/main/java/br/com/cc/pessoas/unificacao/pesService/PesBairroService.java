package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesBairroDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesBairroFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesBairroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesBairroService {
    @Autowired private PesBairroRepository repository;
    public Page<PesBairroDTO> filtrar(PesBairroFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesBairroDTO::fromEntity);
    }

    public List<PesBairroDTO> filtrar(PesBairroFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesBairroDTO::fromEntity)
                .toList();
    }
}