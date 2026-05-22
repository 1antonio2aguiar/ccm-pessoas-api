package br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa;

import java.time.LocalDateTime;
import java.util.List;

public record CadUnicoPessoaOrigemDTO(
        Long id,
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
        LocalDateTime dataCadastro,
        String observacao,
        String cidadeNascimentoNome,
        List<CadUnicoEnderecoOrigemDTO> enderecos
) {}
