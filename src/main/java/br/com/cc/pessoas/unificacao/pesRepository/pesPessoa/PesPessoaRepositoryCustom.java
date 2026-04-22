package br.com.cc.pessoas.unificacao.pesRepository.pesPessoa;

import br.com.cc.pessoas.unificacao.pesDto.PesPessoaDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesPessoaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PesPessoaRepositoryCustom {
    Page<PesPessoaDTO> filtrarCpfUnicoNaoMigradas(PesPessoaFilter filter, Pageable pageable);
    Page<PesPessoaDTO> filtrarCpfDuplicadoNaoMigradas(PesPessoaFilter filter, Pageable pageable);
}