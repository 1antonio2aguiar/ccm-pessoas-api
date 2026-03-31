package br.com.cc.pessoas.unificacao.pesRepository.pesTipoLogradouro;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoLogradouro;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoLogradouroFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesTipoLogradouroRepositoryQuery {
    Page<PesTipoLogradouro> filtrar(PesTipoLogradouroFilter filter, Pageable pageable);

    List<PesTipoLogradouro> filtrar(PesTipoLogradouroFilter filter);
}
