package br.com.cc.pessoas.unificacao.cidade.cidFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CidadeUnificadoFilter {

    private Long cidade;
    private String nome;
    private String uf;

    /*
     * UNICA, DUPLICADA, AMBIGUA ou SEM_CORRESPONDENCIA.
     */
    private String situacao;
}
