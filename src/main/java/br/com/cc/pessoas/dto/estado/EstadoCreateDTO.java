package br.com.cc.pessoas.dto.estado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EstadoCreateDTO(

        @NotNull
        Long paisId,
        @NotBlank
        @Size(max = 255)
        String nome,

        @NotBlank
        @Size(max = 5)
        String uf,

        Long codigoInep
) {

    public String getNome() {
        return nome != null ? nome.toUpperCase() : "";
    }

    public String getSigla() {
        return uf != null ? uf.toUpperCase() : "";
    }
}
