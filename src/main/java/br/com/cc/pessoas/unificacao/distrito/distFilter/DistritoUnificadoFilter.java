package br.com.cc.pessoas.unificacao.distrito.distFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DistritoUnificadoFilter {

    private Long cidade;
    private Long distrito;
    private String nome;
    private String nomeCidade;
    private String uf;

    /*
     * CIDADE_NAO_UNIFICADA, UNICA, DUPLICADA, AMBIGUA ou SEM_CORRESPONDENCIA.
     */
    private String situacao;
}
