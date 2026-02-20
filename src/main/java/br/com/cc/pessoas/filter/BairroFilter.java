package br.com.cc.pessoas.filter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BairroFilter {

    private Long id;
    private String nome;

    private Long cidadeId;
    private String cidadeNome;
}
