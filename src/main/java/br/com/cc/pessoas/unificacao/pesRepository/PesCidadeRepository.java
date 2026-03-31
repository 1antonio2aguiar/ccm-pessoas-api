package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pesEntity.PesCidade;
import br.com.cc.pessoas.unificacao.pesRepository.pesCidade.PesCidadeRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesCidadeRepository
        extends JpaRepository<PesCidade, Long>, PesCidadeRepositoryQuery {
}
