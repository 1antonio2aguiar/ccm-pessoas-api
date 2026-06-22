package br.com.cc.pessoas.unificacao.saneamento.saneFilter;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
public class SanePessoaFilter {

    private Long pessoa;
    private String nome;
    private Long cpf;
    private Long cnpj;
    private String fisicaJuridica;
    private LocalDate dataNascimento;

    private Boolean somenteCpfUnico;
    private Boolean somenteNaoMigradas;
    private String statusCadastro;
}