package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.repository.pessoa.PessoaRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepository extends JpaRepository<Pessoa, Long>, PessoaRepositoryQuery {
}