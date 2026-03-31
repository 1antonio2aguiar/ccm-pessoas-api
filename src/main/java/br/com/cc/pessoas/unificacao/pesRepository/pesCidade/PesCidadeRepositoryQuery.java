package br.com.cc.pessoas.unificacao.pesRepository.pesCidade;

import br.com.cc.pessoas.entity.Cidade;
import br.com.cc.pessoas.filter.CidadeFilter;
import br.com.cc.pessoas.unificacao.pesEntity.PesCidade;
import br.com.cc.pessoas.unificacao.pesFilter.PesCidadeFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesCidadeRepositoryQuery {
    Page<PesCidade> filtrar(PesCidadeFilter filter, Pageable pageable);

    List<PesCidade> filtrar(PesCidadeFilter filter);
}
