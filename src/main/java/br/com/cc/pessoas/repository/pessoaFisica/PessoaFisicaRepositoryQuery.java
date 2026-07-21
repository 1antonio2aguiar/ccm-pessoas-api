package br.com.cc.pessoas.repository.pessoaFisica;

import br.com.cc.pessoas.dto.pessoa.PessoaDTO;
import br.com.cc.pessoas.filter.PessoaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PessoaFisicaRepositoryQuery {

    List<PessoaDTO> filtrar(PessoaFilter filter);

    Page<PessoaDTO> filtrar(
            PessoaFilter filter,
            Pageable pageable
    );
}