package br.com.cc.pessoas.unificacao.rh.rhFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RhLogradouroFilter {
    private Long logradouro;
    private String nome;

    private Long cidade;
    private String cidadeNome;

    private Long distrito;
    private String distritoNome;

}
