package br.com.cc.pessoas.unificacao.pesRepository.pesTipoPessoa;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoPessoa;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoPessoaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesTipoPessoaRepositoryQuery {
    Page<PesTipoPessoa> filtrar(PesTipoPessoaFilter filter, Pageable pageable);

    List<PesTipoPessoa> filtrar(PesTipoPessoaFilter filter);
}
