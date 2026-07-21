package br.com.cc.pessoas.dto.pessoa;

import java.time.LocalDateTime;
import java.util.List;

public record PessoaUpdateDTO(

        String nome,
        Long tipoPessoaId,
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