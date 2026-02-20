package br.com.cc.pessoas.dto.pessoa;

import java.time.LocalDateTime;

public record PessoaUpdateDTO(

        Long tipoPessoaId,
        Long situacaoId,
        LocalDateTime dataCadastro,
        String observacao,

        DadosPessoaFisicaDTO dadosPessoaFisica,
        DadosPessoaJuridicaDTO dadosPessoaJuridica
) {}
