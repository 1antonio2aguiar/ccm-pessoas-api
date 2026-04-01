package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesPessoaDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesPessoaFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesPessoaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesPessoaService {
    @Autowired private PesPessoaRepository repository;
    public Page<PesPessoaDTO> filtrar(PesPessoaFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesPessoaDTO::fromEntity);
    }

    public List<PesPessoaDTO> filtrar(PesPessoaFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesPessoaDTO::fromEntity)
                .toList();
    }
}