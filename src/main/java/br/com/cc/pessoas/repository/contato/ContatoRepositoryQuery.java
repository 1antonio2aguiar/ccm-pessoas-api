package br.com.cc.pessoas.repository.contato;

import br.com.cc.pessoas.entity.Contato;
import br.com.cc.pessoas.filter.ContatoFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContatoRepositoryQuery {
    List<Contato> filtrar(ContatoFilter filter);

    Page<Contato> filtrar(ContatoFilter filter, Pageable pageable);
}
