package br.com.cc.pessoas.unificacao.rh.rhService;

import br.com.cc.pessoas.unificacao.rh.rhDto.RhPessoaDTO;
import br.com.cc.pessoas.unificacao.rh.rhFilter.RhPessoaFilter;
import br.com.cc.pessoas.unificacao.rh.rhRepository.RhPessoaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RhPessoaService {

    private final RhPessoaRepository repository;

    public Page<RhPessoaDTO> filtrarListaRh(RhPessoaFilter filter, Pageable pageable) {
        return repository.filtrarListaRh(filter, pageable);
    }
}