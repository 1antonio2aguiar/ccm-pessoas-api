package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Cep;
import br.com.cc.pessoas.repository.cep.CepRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CepRepository
        extends JpaRepository<Cep, Long>, CepRepositoryQuery {
}
