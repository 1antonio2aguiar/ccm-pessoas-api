package br.com.cc.pessoas.unificacao.saneamento.saneFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaneCepFilter {

    private Long cep;
    private Long cidade;
    private String cidadeNome;
    private Long logradouro;
    private String logradouroNome;
}
