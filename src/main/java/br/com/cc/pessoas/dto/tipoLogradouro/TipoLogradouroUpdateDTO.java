package br.com.cc.pessoas.dto.tipoLogradouro;

import jakarta.validation.constraints.NotBlank;

public record TipoLogradouroUpdateDTO(
        @NotBlank String descricao,
        @NotBlank String sigla
) {
        public String getDescricao() {
                return descricao.toUpperCase();
        }

        public String getSigla() {
                return sigla.toUpperCase();
        }
}
