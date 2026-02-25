package br.com.cc.pessoas.repository.estado;

import br.com.cc.pessoas.entity.Estado;
import br.com.cc.pessoas.filter.EstadoFilter;

import java.util.List;

public interface EstadoRepositoryQuery {
    List<Estado> filtrar(EstadoFilter filter);
}
