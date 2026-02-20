package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Bairro;
import br.com.cc.pessoas.repository.bairro.BairroRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BairroRepository extends JpaRepository<Bairro, Long>, BairroRepositoryQuery {
}