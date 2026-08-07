package br.com.cc.pessoas.unificacao.pessoa.pesFilter;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PesPessoaFilter {

    private Long pessoa;
    private String nome;
    private String cpf;
    private String cnpj;
    private String fisicaJuridica;
    private LocalDate dataNascimento;

    private Boolean somenteCpfUnico;
    private Boolean somenteNaoMigradas;
}