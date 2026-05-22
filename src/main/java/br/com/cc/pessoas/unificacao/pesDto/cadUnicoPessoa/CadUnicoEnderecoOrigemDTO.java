package br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa;

public record CadUnicoEnderecoOrigemDTO(
        String banco,
        Long cdOrigem,

        Long cidade,
        String cidadeNome,

        Long distrito,
        String distritoNome,

        Long bairro,
        String bairroNome,

        Long logradouro,
        String logradouroNome,

        Long numero,
        String complemento,

        Long cep,

        String uf,
        String tipoLogradouro
) {}