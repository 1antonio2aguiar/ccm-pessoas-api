package br.com.cc.pessoas.unificacao.pesRepository.pesPessoa;

import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.PesTipoPessoa;
import br.com.cc.pessoas.unificacao.pesFilter.PesPessoaFilter;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoPessoaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesPessoaRepositoryQuery {
    Page<PesPessoa> filtrar(PesPessoaFilter filter, Pageable pageable);
    List<PesPessoa> filtrar(PesPessoaFilter filter);
}
