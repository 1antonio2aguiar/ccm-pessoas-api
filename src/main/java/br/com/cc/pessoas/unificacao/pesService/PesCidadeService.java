package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesCidadeDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesCidadeFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesCidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesCidadeService {
    @Autowired private PesCidadeRepository repository;
    public Page<PesCidadeDTO> filtrar(PesCidadeFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesCidadeDTO::fromEntity);
    }

    public List<PesCidadeDTO> filtrar(PesCidadeFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesCidadeDTO::fromEntity)
                .toList();
    }
}