package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesDistritoDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesDistritoFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesDistritoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesDistritoService {
    @Autowired private PesDistritoRepository repository;
    public Page<PesDistritoDTO> filtrar(PesDistritoFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesDistritoDTO::fromEntity);
    }

    public List<PesDistritoDTO> filtrar(PesDistritoFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesDistritoDTO::fromEntity)
                .toList();
    }
}