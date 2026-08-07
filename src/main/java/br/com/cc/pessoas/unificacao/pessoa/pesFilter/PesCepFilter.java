package br.com.cc.pessoas.unificacao.pessoa.pesFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PesCepFilter {

    private Long cep;
    private Long cidade;
    private String cidadeNome;
    private Long logradouro;
    private String logradouroNome;
}
