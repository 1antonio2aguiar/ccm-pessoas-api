package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Estado;
import br.com.cc.pessoas.repository.estado.EstadoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoRepository
        extends JpaRepository<Estado, Long>, EstadoRepositoryQuery {
}
