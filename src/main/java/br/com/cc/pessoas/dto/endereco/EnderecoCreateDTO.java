package br.com.cc.pessoas.dto.endereco;

import jakarta.validation.constraints.NotNull;

public record EnderecoCreateDTO(

        @NotNull
        Long numero,

        @NotNull
        String principal,

        @NotNull
        Integer tipoEndereco,

        String complemento,

        @NotNull
        Long pessoaId,

        Long dadosPessoaJuridicaId,

        Long cepId,

        @NotNull
        Long logradouroId,

        Long bairroId
) {
}