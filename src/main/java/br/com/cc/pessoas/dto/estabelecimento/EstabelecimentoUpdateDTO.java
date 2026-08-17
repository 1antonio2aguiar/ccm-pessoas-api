package br.com.cc.pessoas.dto.estabelecimento;

public record EstabelecimentoUpdateDTO(

        String cnpj,
        String nome,
        Long estabelecimento,
        String nomeFantasia,
        String objetoSocial,
        String microEmpresa,
        String conjuge,
        Long tipoEmpresa

) {
}