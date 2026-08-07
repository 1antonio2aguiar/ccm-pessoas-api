package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesCep;
import br.com.cc.pessoas.unificacao.pesRepository.pesCep.PesCepRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesCepRepository
        extends JpaRepository<PesCep, Long>, PesCepRepositoryQuery {
}
