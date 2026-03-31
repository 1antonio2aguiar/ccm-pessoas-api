package br.com.cc.pessoas.unificacao.pesFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PesCepFilter {

    private String cep;
    private Long cidade;
    private String cidadeNome;
    private Long logradouro;
    private String logradouroNome;
}
