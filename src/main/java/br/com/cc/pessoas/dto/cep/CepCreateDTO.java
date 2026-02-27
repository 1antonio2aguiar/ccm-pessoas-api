package br.com.cc.pessoas.dto.cep;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CepCreateDTO(

        @NotNull Long logradouroId,
        @NotNull Long bairroId,

        @NotBlank
        @Size(min = 8, max = 8)
        String cep,

        Integer numeroIni,
        Integer numeroFim,
        String identificacao
) {
    public String getIdentificacao() {
        return identificacao != null ? identificacao.toUpperCase() : "";
    }
    public String getCep() {
        return cep != null ? cep.replaceAll("\\D", "") : "";
    }
}
