package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesEstado;
import br.com.cc.pessoas.unificacao.pesRepository.pesEstado.PesEstadoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesEstadoRepository
        extends JpaRepository<PesEstado, Long>, PesEstadoRepositoryQuery {
}
