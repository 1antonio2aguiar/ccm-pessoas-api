package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pesEntity.PesCep;
import br.com.cc.pessoas.unificacao.pesEntity.PesPais;
import br.com.cc.pessoas.unificacao.pesRepository.pesCep.PesCepRepositoryQuery;
import br.com.cc.pessoas.unificacao.pesRepository.pesPais.PesPaisRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesCepRepository
        extends JpaRepository<PesCep, Long>, PesCepRepositoryQuery {
}
