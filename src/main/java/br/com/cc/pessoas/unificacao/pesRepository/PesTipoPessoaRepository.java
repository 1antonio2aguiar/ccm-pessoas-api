package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoPessoa;
import br.com.cc.pessoas.unificacao.pesRepository.pesTipoPessoa.PesTipoPessoaRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesTipoPessoaRepository
        extends JpaRepository<PesTipoPessoa, Integer>, PesTipoPessoaRepositoryQuery {
}
