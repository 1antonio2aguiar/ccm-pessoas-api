package br.com.cc.pessoas.dto.bairro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BairroCreateDTO(

        @NotNull Long distritoId,
        @NotBlank
        @Size(max = 255)
        String nome,
        String nomeAbreviado


) {

    public String getNome() {
        return nome != null ? nome.toUpperCase() : "";
    }
    public String getNomeAbrevidado() {
        return nomeAbreviado != null ? nomeAbreviado.toUpperCase() : "";
    }

}
