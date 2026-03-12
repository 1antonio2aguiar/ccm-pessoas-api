package br.com.cc.pessoas.dto.endereco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnderecoUpdateDTO(

        Long numero,
        String principal,
        Integer tipoEndereco,
        String complemento,
        Long cepId,
        Long logradouroId,
        Long bairroId
) {

}
