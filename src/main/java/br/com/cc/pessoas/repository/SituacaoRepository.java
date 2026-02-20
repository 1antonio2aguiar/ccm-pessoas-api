package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.entity.Situacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SituacaoRepository
        extends JpaRepository<Situacao, Long> , JpaSpecificationExecutor<Pessoa> {
}
