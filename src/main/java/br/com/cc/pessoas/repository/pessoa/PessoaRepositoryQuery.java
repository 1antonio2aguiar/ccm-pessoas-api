package br.com.cc.pessoas.repository.pessoa;

import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.filter.PessoaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PessoaRepositoryQuery {

    List<Pessoa> filtrar(PessoaFilter filter);

    Page<Pessoa> filtrar(PessoaFilter filter, Pageable pageable);
}
