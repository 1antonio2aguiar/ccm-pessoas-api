package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesTipoPessoaDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoPessoaFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesTipoPessoaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesTipoPessoaService {
    @Autowired private PesTipoPessoaRepository repository;
    public Page<PesTipoPessoaDTO> filtrar(PesTipoPessoaFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesTipoPessoaDTO::fromEntity);
    }

    public List<PesTipoPessoaDTO> filtrar(PesTipoPessoaFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesTipoPessoaDTO::fromEntity)
                .toList();
    }
}