package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.TipoPessoa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoPessoaRepository
        extends JpaRepository<TipoPessoa, Long> {
}
