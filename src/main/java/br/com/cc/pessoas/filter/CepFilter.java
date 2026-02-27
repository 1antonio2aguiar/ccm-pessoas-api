package br.com.cc.pessoas.filter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CepFilter {

    private Long id;
    private String cep;

    private Long cidadeId;
    private String cidadeNome;

    private String bairroNome;
    private String logradouroNome;
}
