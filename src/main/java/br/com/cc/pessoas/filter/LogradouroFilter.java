package br.com.cc.pessoas.filter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogradouroFilter {

    private Long id;
    private String nome;

    private Long cidadeId;
    private String cidadeNome;
}
