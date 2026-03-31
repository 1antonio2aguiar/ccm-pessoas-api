package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesEstadoDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesEstadoFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesEstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesEstadoService {
    @Autowired private PesEstadoRepository repository;
    public Page<PesEstadoDTO> filtrar(PesEstadoFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesEstadoDTO::fromEntity);
    }

    public List<PesEstadoDTO> filtrar(PesEstadoFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesEstadoDTO::fromEntity)
                .toList();
    }
}