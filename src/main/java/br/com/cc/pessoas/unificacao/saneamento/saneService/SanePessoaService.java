package br.com.cc.pessoas.unificacao.saneamento.saneService;

import br.com.cc.pessoas.unificacao.saneamento.saneDto.SanePessoaDTO;
import br.com.cc.pessoas.unificacao.saneamento.saneFilter.SanePessoaFilter;
import br.com.cc.pessoas.unificacao.saneamento.saneRepository.SanePessoaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SanePessoaService {

    private final SanePessoaRepository repository;

    public Page<SanePessoaDTO> filtrarListaSaneCpfUnico(SanePessoaFilter filter, Pageable pageable) {
        return repository.filtrarListaSaneCpfUnico(filter, pageable);
    }
    public Page<SanePessoaDTO> filtrarListaSaneCnpjUnico(SanePessoaFilter filter, Pageable pageable) {
        return repository.filtrarListaSaneCnpjUnico(filter, pageable);
    }
}