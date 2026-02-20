package br.com.cc.pessoas.dto.cidade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CidadeCreateDTO(

        @NotNull
        Long estadoId,

        @NotBlank
        @Size(max = 255)
        String nome,

        @Size(max = 20)
        String sigla,

        Long codigoSicom,
        Long codigoIbge,
        Long codigoInep
) {

    public String getNome() {
        return nome != null ? nome.toUpperCase() : "";
    }

    public String getSigla() {
        return sigla != null ? sigla.toUpperCase() : "";
    }
}
