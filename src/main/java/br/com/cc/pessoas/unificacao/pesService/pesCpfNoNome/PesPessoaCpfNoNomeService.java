package br.com.cc.pessoas.unificacao.pesService.pesCpfNoNome;

import br.com.cc.pessoas.unificacao.pessoa.pesDto.pesCpfNoNome.PesPessoaCpfNoNomeDTO;
import br.com.cc.pessoas.unificacao.pessoa.pesRepository.PesPessoaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class PesPessoaCpfNoNomeService {

    private final PesPessoaRepository pesPessoaRepository;

    /*public Page<PesPessoaCpfNoNomeDTO> listarCpfNoNome(String nome, Pageable pageable) {
        return pesPessoaRepository.listarCpfNoNome(nome, pageable);
    }*/
}