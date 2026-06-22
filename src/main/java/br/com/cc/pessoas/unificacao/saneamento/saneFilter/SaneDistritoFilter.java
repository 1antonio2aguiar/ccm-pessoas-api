package br.com.cc.pessoas.unificacao.saneamento.saneFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaneDistritoFilter {
    private Long distrito;
    private String nome;
    private Long cidade;
    private String cidadeNome;
}
