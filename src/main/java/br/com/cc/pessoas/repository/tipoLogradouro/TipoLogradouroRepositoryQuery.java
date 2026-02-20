package br.com.cc.pessoas.repository.tipoLogradouro;

import br.com.cc.pessoas.entity.TipoLogradouro;
import br.com.cc.pessoas.filter.TipoLogradouroFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoLogradouroRepositoryQuery {

    Page<TipoLogradouro> filtrar(TipoLogradouroFilter filter, Pageable pageable);

    List<TipoLogradouro> filtrar(TipoLogradouroFilter filter);
}

