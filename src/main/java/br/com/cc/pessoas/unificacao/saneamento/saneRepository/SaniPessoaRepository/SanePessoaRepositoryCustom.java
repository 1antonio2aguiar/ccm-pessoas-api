package br.com.cc.pessoas.unificacao.saneamento.saneRepository.SaniPessoaRepository;

import br.com.cc.pessoas.unificacao.saneamento.saneDto.SanePessoaDTO;
import br.com.cc.pessoas.unificacao.saneamento.saneFilter.SanePessoaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface SanePessoaRepositoryCustom {

    Page<SanePessoaDTO> filtrarListaSaneCpfUnico(SanePessoaFilter filter, Pageable pageable);
    Page<SanePessoaDTO> filtrarListaSaneCnpjUnico(SanePessoaFilter filter, Pageable pageable);
}