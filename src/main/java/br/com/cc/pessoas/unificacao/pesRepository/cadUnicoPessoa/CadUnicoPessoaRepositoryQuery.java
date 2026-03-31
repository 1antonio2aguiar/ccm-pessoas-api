package br.com.cc.pessoas.unificacao.pesRepository.cadUnicoPessoa;

import br.com.cc.pessoas.unificacao.pesEntity.CadUnicoPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesFilter.CadUnicoPessoaFilter;
import br.com.cc.pessoas.unificacao.pesFilter.PesPessoaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CadUnicoPessoaRepositoryQuery {
    Page<CadUnicoPessoa> filtrar(CadUnicoPessoaFilter filter, Pageable pageable);

    List<CadUnicoPessoa> filtrar(CadUnicoPessoaFilter filter);
}
