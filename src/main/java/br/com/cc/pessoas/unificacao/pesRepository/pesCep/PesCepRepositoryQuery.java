package br.com.cc.pessoas.unificacao.pesRepository.pesCep;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesCep;
import br.com.cc.pessoas.unificacao.pessoa.pesFilter.PesCepFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesCepRepositoryQuery {
    Page<PesCep> filtrar(PesCepFilter filter, Pageable pageable);

    List<PesCep> filtrar(PesCepFilter filter);
}
