package br.com.cc.pessoas.unificacao.saneamento.saneFilter;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class SaneBairroFilter {
    private Long bairro;
    private String nome;

    private Long cidade;
    private String cidadeNome;

    private Long distrito;
    private String distritoNome;

}
