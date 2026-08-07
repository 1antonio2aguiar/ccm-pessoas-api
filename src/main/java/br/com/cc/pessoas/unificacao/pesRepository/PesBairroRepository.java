package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesBairro;
import br.com.cc.pessoas.unificacao.pesRepository.pesBairro.PesBairroRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesBairroRepository
        extends JpaRepository<PesBairro, Long>, PesBairroRepositoryQuery {
}
