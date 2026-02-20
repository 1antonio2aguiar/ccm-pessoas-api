package br.com.cc.pessoas.repository.pais;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import br.com.cc.pessoas.entity.Pais;
import br.com.cc.pessoas.filter.PaisFilter;

@Repository
public interface PaisRepositoryQuery {

    Page<Pais> filtrar(PaisFilter filter, Pageable pageable);

    List<Pais> filtrar(PaisFilter filter);
}

