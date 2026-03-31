package br.com.cc.pessoas.dto.contato;

import jakarta.validation.constraints.NotNull;

public record ContatoCreateDTO(
        @NotNull
        String contato,
        @NotNull
        Integer tipoContato,
        @NotNull
        String principal,
        String complemento,
        @NotNull
        Long pessoaId
) {

}
