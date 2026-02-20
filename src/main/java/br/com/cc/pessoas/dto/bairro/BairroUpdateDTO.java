package br.com.cc.pessoas.dto.bairro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BairroUpdateDTO(
        @NotBlank String nome,
        String nomeAbreviado
) {
    public String getNome() {
        return nome != null ? nome.toUpperCase() : "";
    }
    public String getNomeAbreviado() {
        return nomeAbreviado != null ? nomeAbreviado.toUpperCase() : "";
    }
}
