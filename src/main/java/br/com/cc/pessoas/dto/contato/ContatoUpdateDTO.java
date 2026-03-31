package br.com.cc.pessoas.dto.contato;

import jakarta.validation.constraints.NotNull;

public record ContatoUpdateDTO(
        @NotNull
        String contato,
        @NotNull
        String principal,
        String complemento
) {

}
