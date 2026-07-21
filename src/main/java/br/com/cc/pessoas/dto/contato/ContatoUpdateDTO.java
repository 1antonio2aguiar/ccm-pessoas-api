package br.com.cc.pessoas.dto.contato;

import jakarta.validation.constraints.NotNull;

public record ContatoUpdateDTO(
        @NotNull
        String contato,
        Integer tipoContato,
        @NotNull
        String principal,
        String complemento,
        Long dadosPessoaJuridicaId
) {

}
