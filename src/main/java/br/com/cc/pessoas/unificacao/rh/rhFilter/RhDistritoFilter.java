package br.com.cc.pessoas.unificacao.rh.rhFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RhDistritoFilter {
    private Long distrito;
    private String nome;
    private Long cidade;
    private String cidadeNome;
}
