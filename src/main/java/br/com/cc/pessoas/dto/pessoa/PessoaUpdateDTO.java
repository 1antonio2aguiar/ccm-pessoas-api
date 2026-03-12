package br.com.cc.pessoas.dto.pessoa;

import java.time.LocalDateTime;

public record PessoaUpdateDTO(
        String nome,
        Long tipoPessoaId,
        Long situacaoId,
        LocalDateTime dataCadastro,
        String observacao,

        DadosPessoaFisicaDTO dadosPessoaFisica,
        DadosPessoaJuridicaDTO dadosPessoaJuridica
) {
    public String getNome() {
        return nome != null ? nome.toUpperCase() : "";
    }
}
