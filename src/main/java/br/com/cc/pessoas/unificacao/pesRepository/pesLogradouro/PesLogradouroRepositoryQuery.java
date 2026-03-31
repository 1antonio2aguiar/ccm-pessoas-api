package br.com.cc.pessoas.unificacao.pesRepository.pesLogradouro;

import br.com.cc.pessoas.unificacao.pesEntity.PesBairro;
import br.com.cc.pessoas.unificacao.pesEntity.PesLogradouro;
import br.com.cc.pessoas.unificacao.pesFilter.PesBairroFilter;
import br.com.cc.pessoas.unificacao.pesFilter.PesLogradouroFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesLogradouroRepositoryQuery {
    Page<PesLogradouro> filtrar(PesLogradouroFilter filter, Pageable pageable);

    List<PesLogradouro> filtrar(PesLogradouroFilter filter);
}
