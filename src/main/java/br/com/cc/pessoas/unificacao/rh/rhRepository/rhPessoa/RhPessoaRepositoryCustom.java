package br.com.cc.pessoas.unificacao.rh.rhRepository.rhPessoa;

import br.com.cc.pessoas.unificacao.rh.rhDto.RhPessoaDTO;
import br.com.cc.pessoas.unificacao.rh.rhFilter.RhPessoaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RhPessoaRepositoryCustom {

    Page<RhPessoaDTO> filtrarListaRh(RhPessoaFilter filter, Pageable pageable);

    List<Long> buscarGrupoDuplicadoCpfRh(Long pessoaId);
}