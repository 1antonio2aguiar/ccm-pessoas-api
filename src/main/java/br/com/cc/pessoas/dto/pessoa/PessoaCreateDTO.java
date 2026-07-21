package br.com.cc.pessoas.dto.pessoa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record PessoaCreateDTO(

        @NotBlank String nome,
        @NotNull Long tipoPessoaId,
        @NotBlank String fisicaJuridica,
        Long situacaoId,
        LocalDateTime dataCadastro,
        String observacao,

        DadosPessoaFisicaDTO dadosPessoaFisica,
        List<DadosPessoaJuridicaDTO> dadosPessoasJuridicas

) {

    public String getNome() {
        return nome != null ? nome.toUpperCase() : "";
    }
}