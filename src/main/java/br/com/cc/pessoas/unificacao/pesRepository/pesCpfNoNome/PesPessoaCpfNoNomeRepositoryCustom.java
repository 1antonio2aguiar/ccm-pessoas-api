package br.com.cc.pessoas.unificacao.pesRepository.pesCpfNoNome;

import br.com.cc.pessoas.unificacao.pesDto.pesCpfNoNome.PesPessoaCpfNoNomeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PesPessoaCpfNoNomeRepositoryCustom {
    Page<PesPessoaCpfNoNomeDTO> listarCpfNoNome(String nome, Pageable pageable);
}