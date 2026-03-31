package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pesEntity.PesPais;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesRepository.pesPais.PesPaisRepositoryQuery;
import br.com.cc.pessoas.unificacao.pesRepository.pesPessoa.PesPessoaRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesPessoaRepository
        extends JpaRepository<PesPessoa, Long>, PesPessoaRepositoryQuery {
}
