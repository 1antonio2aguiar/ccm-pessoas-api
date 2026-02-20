package br.com.cc.pessoas.repository.distrito;

import br.com.cc.pessoas.entity.Distrito;
import br.com.cc.pessoas.filter.DistritoFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DistritoRepositoryQuery {

    Page<Distrito> filtrar(DistritoFilter filter, Pageable pageable);

    List<Distrito> filtrar(DistritoFilter filter);
}
