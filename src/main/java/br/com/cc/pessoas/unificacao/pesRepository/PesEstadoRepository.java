package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pesEntity.PesEstado;
import br.com.cc.pessoas.unificacao.pesEntity.PesPais;
import br.com.cc.pessoas.unificacao.pesRepository.pesEstado.PesEstadoRepositoryQuery;
import br.com.cc.pessoas.unificacao.pesRepository.pesPais.PesPaisRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesEstadoRepository
        extends JpaRepository<PesEstado, Long>, PesEstadoRepositoryQuery {
}
