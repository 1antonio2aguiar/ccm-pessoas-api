package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesCepDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesCepFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesCepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesCepService {
    @Autowired private PesCepRepository repository;
    public Page<PesCepDTO> filtrar(PesCepFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesCepDTO::fromEntity);
    }

    public List<PesCepDTO> filtrar(PesCepFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesCepDTO::fromEntity)
                .toList();
    }
}