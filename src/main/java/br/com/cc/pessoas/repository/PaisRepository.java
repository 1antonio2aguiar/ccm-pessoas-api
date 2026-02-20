package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Pais;
import br.com.cc.pessoas.repository.pais.PaisRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaisRepository
        extends JpaRepository<Pais, Long>, PaisRepositoryQuery {
}