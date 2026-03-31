package br.com.cc.pessoas.unificacao.pesRepository.pesDistrito;

import br.com.cc.pessoas.unificacao.pesEntity.PesDistrito;
import br.com.cc.pessoas.unificacao.pesFilter.PesDistritoFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesDistritoRepositoryQuery {
    Page<PesDistrito> filtrar(PesDistritoFilter filter, Pageable pageable);

    List<PesDistrito> filtrar(PesDistritoFilter filter);
}
