package br.com.cc.pessoas.repository.cep;

import br.com.cc.pessoas.entity.Cep;
import br.com.cc.pessoas.filter.CepFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CepRepositoryQuery {

    List<Cep> filtrar(CepFilter filter);

    Page<Cep> filtrar(CepFilter filter, Pageable pageable);
}
