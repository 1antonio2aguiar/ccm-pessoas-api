package br.com.cc.pessoas.repository.bairro;

import br.com.cc.pessoas.entity.Bairro;
import br.com.cc.pessoas.filter.BairroFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BairroRepositoryQuery {

    List<Bairro> filtrar(BairroFilter filter);

    Page<Bairro> filtrar(BairroFilter filter, Pageable pageable);
}
