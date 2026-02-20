package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.TipoLogradouro;
import br.com.cc.pessoas.repository.tipoLogradouro.TipoLogradouroRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoLogradouroRepository
        extends JpaRepository<TipoLogradouro, Long>, TipoLogradouroRepositoryQuery {
}