package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Logradouro;
import br.com.cc.pessoas.repository.logradouro.LogradouroRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogradouroRepository
        extends JpaRepository<Logradouro, Long>, LogradouroRepositoryQuery {
}
