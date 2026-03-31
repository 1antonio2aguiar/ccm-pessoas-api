package br.com.cc.pessoas.unificacao.pesFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PesLogradouroFilter {
    private Long logradouro;
    private String nome;

    private Long cidade;
    private String cidadeNome;

    private Long distrito;
    private String distritoNome;

}
