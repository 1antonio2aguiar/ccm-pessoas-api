package br.com.cc.pessoas.dto.estabelecimento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EstabelecimentoCreateDTO(

        @NotNull
        Long pessoaId,

        @NotBlank
        String cnpj,

        @NotBlank
        String nome,

        Long estabelecimento,
        String nomeFantasia,
        String objetoSocial,
        String microEmpresa,
        String conjuge,
        Long tipoEmpresa

) {
}