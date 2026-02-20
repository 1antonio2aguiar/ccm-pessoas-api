package br.com.cc.pessoas.dto.logradouro;

import jakarta.validation.constraints.NotNull;

public record LogradouroUpdateDTO(
        @NotNull Long tipoLogradouroId,
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
