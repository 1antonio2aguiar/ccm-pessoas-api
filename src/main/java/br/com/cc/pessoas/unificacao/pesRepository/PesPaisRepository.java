package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPais;
import br.com.cc.pessoas.unificacao.pesRepository.pesPais.PesPaisRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesPaisRepository
        extends JpaRepository<PesPais, Long>, PesPaisRepositoryQuery {
}
