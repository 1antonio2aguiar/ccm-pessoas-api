package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Distrito;
import br.com.cc.pessoas.repository.distrito.DistritoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistritoRepository
        extends JpaRepository<Distrito, Long>, DistritoRepositoryQuery {
}
