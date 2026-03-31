package br.com.cc.pessoas.unificacao.pesFilter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class CadUnicoPessoaFilter {
    private Long id;
    private Long cd_origem;
    private String nome;
    private Long cpf;
    private Long cnpj;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // yyyy-MM-dd
    private LocalDate dataNascimento;
}
