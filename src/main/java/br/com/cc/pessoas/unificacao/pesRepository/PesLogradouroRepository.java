package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesLogradouro;
import br.com.cc.pessoas.unificacao.pesRepository.pesLogradouro.PesLogradouroRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesLogradouroRepository
        extends JpaRepository<PesLogradouro, Long>, PesLogradouroRepositoryQuery {
}
