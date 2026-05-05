package br.com.cc.pessoas.unificacao.rh.rhFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RhCepFilter {

    private Long cep;
    private Long cidade;
    private String cidadeNome;
    private Long logradouro;
    private String logradouroNome;
}
