package br.com.cc.pessoas.unificacao.pessoa.pesDto;

import java.time.LocalDate;

public record PesEmpresaVinculadaDTO(
        Long estabelecimento,
        Long pessoa,
        String nome,
        Long cnpj,
        Long tipoPessoa,
        Long cidade,
        Long distrito,
        Long bairro,
        Long logradouro,
        Integer numero,
        String complemento,
        Long cep,
        Long tipoDocumento,
        String numeroDocumento,
        String telefone,
        String celular,
        String email,
        String inscricaoEstadual,
        String fantasia,
        String objetoSocial,
        String microempresa,
        LocalDate dataCadastro,
        String observacao

) {
}