package br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa;

import java.time.LocalDateTime;

public record CadUnicoPessoaCreateDTO(

        Long cdOrigem,
        Long tipoPessoa,
        String nome,
        String fisicaJuridica,
        Long cpfCnpj,
        String estadoCivil,
        String sexo,
        String email,
        String banco,
        Long pessoasCdUnico,
        String status,
        LocalDateTime dataNascimento,
        String observacao,
        Long cidadeNascimento

) {}