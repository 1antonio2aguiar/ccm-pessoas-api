package br.com.cc.pessoas.repository.estabelecimento;

import br.com.cc.pessoas.entity.Logradouro;
import br.com.cc.pessoas.filter.LogradouroFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import br.com.cc.pessoas.dto.estabelecimento.EstabelecimentoDTO;
import br.com.cc.pessoas.filter.PessoaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EstabelecimentoRepositoryQuery {
    List<EstabelecimentoDTO> filtrar(PessoaFilter filter);
    Page<EstabelecimentoDTO> filtrar(PessoaFilter filter,Pageable pageable);
}