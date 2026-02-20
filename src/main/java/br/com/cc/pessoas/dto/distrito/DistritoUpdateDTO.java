package br.com.cc.pessoas.dto.distrito;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DistritoUpdateDTO(

        @NotNull
        Long cidadeId,

        @NotBlank
        @Size(max = 255)
        String nome,

        Long codigoInep
) {
    public String getNome() {
        return nome != null ? nome.toUpperCase() : "";
    }
}
