package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoLogradouro;
import br.com.cc.pessoas.unificacao.pesRepository.pesTipoLogradouro.PesTipoLogradouroRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesTipoLogradouroRepository
        extends JpaRepository<PesTipoLogradouro, String>, PesTipoLogradouroRepositoryQuery {
}
