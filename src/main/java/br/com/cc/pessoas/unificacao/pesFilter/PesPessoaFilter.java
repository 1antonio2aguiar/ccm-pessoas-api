package br.com.cc.pessoas.unificacao.pesFilter;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PesPessoaFilter {

    private Long pessoa;
    private String nome;
    private Long cpf;
    private Long cnpj;
    private String fisicaJuridica;
    private LocalDate dataNascimento;

    private Boolean somenteCpfUnico;
    private Boolean somenteNaoMigradas;
}