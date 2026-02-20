package br.com.cc.pessoas.repository.cidade;

import br.com.cc.pessoas.entity.Cidade;
import br.com.cc.pessoas.filter.CidadeFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CidadeRepositoryQuery {
    Page<Cidade> filtrar(CidadeFilter filter, Pageable pageable);

    List<Cidade> filtrar(CidadeFilter filter);
}
