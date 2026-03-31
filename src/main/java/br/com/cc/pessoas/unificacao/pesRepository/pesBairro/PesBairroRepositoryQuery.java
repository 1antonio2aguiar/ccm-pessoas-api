package br.com.cc.pessoas.unificacao.pesRepository.pesBairro;

import br.com.cc.pessoas.unificacao.pesEntity.PesBairro;
import br.com.cc.pessoas.unificacao.pesEntity.PesDistrito;
import br.com.cc.pessoas.unificacao.pesFilter.PesBairroFilter;
import br.com.cc.pessoas.unificacao.pesFilter.PesDistritoFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesBairroRepositoryQuery {
    Page<PesBairro> filtrar(PesBairroFilter filter, Pageable pageable);

    List<PesBairro> filtrar(PesBairroFilter filter);
}
