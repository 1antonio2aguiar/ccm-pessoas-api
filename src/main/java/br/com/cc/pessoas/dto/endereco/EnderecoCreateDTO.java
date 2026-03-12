package br.com.cc.pessoas.dto.endereco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
        Long cepId,
        @NotNull
        Long logradouroId,
        Long bairroId
) {

}
