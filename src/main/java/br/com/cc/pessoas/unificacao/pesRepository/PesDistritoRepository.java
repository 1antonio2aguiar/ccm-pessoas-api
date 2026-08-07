package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesDistrito;
import br.com.cc.pessoas.unificacao.pesRepository.pesDistrito.PesDistritoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesDistritoRepository
        extends JpaRepository<PesDistrito, Long>, PesDistritoRepositoryQuery {
}
