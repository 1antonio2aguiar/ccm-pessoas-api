package br.com.cc.pessoas.unificacao.bairro.bairFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BairroUnificadoFilter {

    private Long cidade;
    private Long distrito;
    private Long bairro;
    private String nome;
    private String nomeCidade;
    private String uf;

    /*
     * DISTRITO_NAO_UNIFICADO, UNICA, DUPLICADA, AMBIGUA ou SEM_CORRESPONDENCIA.
     */
    private String situacao;
}
