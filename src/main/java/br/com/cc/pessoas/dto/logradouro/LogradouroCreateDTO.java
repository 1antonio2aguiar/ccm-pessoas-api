package br.com.cc.pessoas.dto.logradouro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LogradouroCreateDTO(

        @NotNull Long distritoId,
        @NotNull Long tipoLogradouroId,

        @NotBlank
        @Size(max = 255)
        String nome,

        String preposicao,
        String tituloPatente,
        String nomeReduzido,
        String nomeSimplificado,
        String complemento
) {

    public String getNome() {
        return nome != null ? nome.toUpperCase() : "";
    }
    public String getPreposicao() {
        return preposicao != null ? preposicao.toUpperCase() : "";
    }
    public String getTituloPatente() {
        return tituloPatente != null ? tituloPatente.toUpperCase() : "";
    }
    public String getNomeReduzido() {
        return nomeReduzido != null ? nomeReduzido.toUpperCase() : "";
    }
    public String getNomeSimplificado() {
        return nomeSimplificado != null ? nomeSimplificado.toUpperCase() : "";
    }
    public String getComplemento() {
        return complemento != null ? complemento.toUpperCase() : "";
    }
}
