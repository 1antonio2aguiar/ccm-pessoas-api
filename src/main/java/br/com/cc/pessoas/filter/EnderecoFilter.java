package com.pesoas.api.filter.enderecos;
import lombok.Data;

@Data
public class EnderecoFilter {
    private Long id;
    private String cep;
    private String logradouro;
    private String pessoa;

}
