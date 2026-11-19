package br.com.cc.pessoas.unificacao.logradouro.logrFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogradouroUnificadoFilter {

    private Long cidade;
    private Long distrito;
    private Long logradouro;
    private String nome;
    private String nomeCidade;
    private String uf;

    /*
     * DISTRITO_NAO_UNIFICADO, UNICA, FAIXAS, DUPLICADA, TIPO_DIFERENTE,
     * AMBIGUA ou SEM_CORRESPONDENCIA.
     */
    private String situacao;
}
