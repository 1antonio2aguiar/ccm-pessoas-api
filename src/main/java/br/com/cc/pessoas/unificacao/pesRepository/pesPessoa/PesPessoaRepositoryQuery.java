package br.com.cc.pessoas.unificacao.pesRepository.pesPessoa;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pessoa.pesFilter.PesPessoaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesPessoaRepositoryQuery {
    Page<PesPessoa> filtrar(PesPessoaFilter filter, Pageable pageable);
    List<PesPessoa> filtrar(PesPessoaFilter filter);
}
