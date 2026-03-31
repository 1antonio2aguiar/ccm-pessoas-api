package br.com.cc.pessoas.unificacao.pesRepository.pesEstado;

import br.com.cc.pessoas.unificacao.pesEntity.PesEstado;
import br.com.cc.pessoas.unificacao.pesEntity.PesPais;
import br.com.cc.pessoas.unificacao.pesFilter.PesEstadoFilter;
import br.com.cc.pessoas.unificacao.pesFilter.PesPaisFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesEstadoRepositoryQuery {
    Page<PesEstado> filtrar(PesEstadoFilter filter, Pageable pageable);

    List<PesEstado> filtrar(PesEstadoFilter filter);
}
