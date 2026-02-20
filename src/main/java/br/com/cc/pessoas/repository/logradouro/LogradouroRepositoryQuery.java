package br.com.cc.pessoas.repository.logradouro;

import br.com.cc.pessoas.entity.Logradouro;
import br.com.cc.pessoas.filter.LogradouroFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LogradouroRepositoryQuery {

    List<Logradouro> filtrar(LogradouroFilter filter);

    Page<Logradouro> filtrar(LogradouroFilter filter, Pageable pageable);
}
