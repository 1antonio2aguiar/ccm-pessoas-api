package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Cidade;
import br.com.cc.pessoas.repository.cidade.CidadeRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CidadeRepository
        extends JpaRepository<Cidade, Long>, CidadeRepositoryQuery {
}

