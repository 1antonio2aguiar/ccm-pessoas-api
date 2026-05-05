package br.com.cc.pessoas.unificacao.rh.rhFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RhCidadeFilter {
    private Long cidade;
    private Long estadoId;
    private String nome;
}
