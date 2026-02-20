package br.com.cc.pessoas.filter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CidadeFilter {

    private Long id;
    private Long estadoId;
    private String nome;
}
