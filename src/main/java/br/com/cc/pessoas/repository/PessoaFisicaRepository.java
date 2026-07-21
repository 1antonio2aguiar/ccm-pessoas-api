package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.DadosPessoaFisica;
import br.com.cc.pessoas.repository.pessoaFisica.PessoaFisicaRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaFisicaRepository
        extends JpaRepository<DadosPessoaFisica, Long>,
        PessoaFisicaRepositoryQuery {
}