package br.com.cc.pessoas.unificacao.pesRepository.pesPais;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPais;
import br.com.cc.pessoas.unificacao.pessoa.pesFilter.PesPaisFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesPaisRepositoryQuery {
    Page<PesPais> filtrar(PesPaisFilter filter, Pageable pageable);

    List<PesPais> filtrar(PesPaisFilter filter);
}
