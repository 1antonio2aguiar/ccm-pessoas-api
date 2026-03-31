package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesLogradouroDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesLogradouroFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesLogradouroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesLogradouroService {
    @Autowired private PesLogradouroRepository repository;
    public Page<PesLogradouroDTO> filtrar(PesLogradouroFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesLogradouroDTO::fromEntity);
    }

    public List<PesLogradouroDTO> filtrar(PesLogradouroFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesLogradouroDTO::fromEntity)
                .toList();
    }
}