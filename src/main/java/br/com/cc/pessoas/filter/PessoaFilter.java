package br.com.cc.pessoas.filter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
public class PessoaFilter {

    private Long id;
    private String nome;
    private String cpf;
    private String cnpj;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // yyyy-MM-dd
    private LocalDate dataNascimento;
}
