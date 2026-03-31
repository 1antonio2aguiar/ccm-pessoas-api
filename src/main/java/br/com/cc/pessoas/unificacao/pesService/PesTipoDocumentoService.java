package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.PesTipoDocumentoDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoDocumentoFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesTipoDocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesTipoDocumentoService {
    @Autowired private PesTipoDocumentoRepository repository;
    public Page<PesTipoDocumentoDTO> filtrar(PesTipoDocumentoFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(PesTipoDocumentoDTO::fromEntity);
    }

    public List<PesTipoDocumentoDTO> filtrar(PesTipoDocumentoFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(PesTipoDocumentoDTO::fromEntity)
                .toList();
    }
}