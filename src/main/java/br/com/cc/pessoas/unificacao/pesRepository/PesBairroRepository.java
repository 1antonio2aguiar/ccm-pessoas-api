package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pesEntity.PesBairro;
import br.com.cc.pessoas.unificacao.pesEntity.PesDistrito;
import br.com.cc.pessoas.unificacao.pesRepository.pesBairro.PesBairroRepositoryQuery;
import br.com.cc.pessoas.unificacao.pesRepository.pesDistrito.PesDistritoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesBairroRepository
        extends JpaRepository<PesBairro, Long>, PesBairroRepositoryQuery {
}
