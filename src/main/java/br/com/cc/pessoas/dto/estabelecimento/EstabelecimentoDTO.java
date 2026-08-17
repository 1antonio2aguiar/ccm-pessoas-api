package br.com.cc.pessoas.dto.estabelecimento;

import br.com.cc.pessoas.entity.DadosPessoaFisica;
import br.com.cc.pessoas.entity.DadosPessoaJuridica;
import br.com.cc.pessoas.entity.Pessoa;

public record EstabelecimentoDTO(

        Long id,
        String cnpj,
        String nome,
        Long estabelecimento,
        String nomeFantasia,
        String objetoSocial,
        String microEmpresa,
        String conjuge,
        Integer mesEnvioSicom,
        Integer anoEnvioSicom,
        Long tipoEmpresa,

        Long pessoaId,
        String pessoaNome,
        String pessoaCpf

) {

    public static EstabelecimentoDTO fromEntity(
            DadosPessoaJuridica dadosPessoaJuridica) {

        if (dadosPessoaJuridica == null) {
            return null;
        }

        Pessoa pessoa = dadosPessoaJuridica.getPessoa();

        DadosPessoaFisica dadosPessoaFisica =
                pessoa != null
                        ? pessoa.getDadosPessoaFisica()
                        : null;

        return new EstabelecimentoDTO(
                dadosPessoaJuridica.getId(),
                dadosPessoaJuridica.getCnpj(),
                dadosPessoaJuridica.getNome(),
                dadosPessoaJuridica.getEstabelecimento(),
                dadosPessoaJuridica.getNomeFantasia(),
                dadosPessoaJuridica.getObjetoSocial(),
                dadosPessoaJuridica.getMicroEmpresa(),
                dadosPessoaJuridica.getConjuge(),
                dadosPessoaJuridica.getMesEnvioSicom(),
                dadosPessoaJuridica.getAnoEnvioSicom(),
                dadosPessoaJuridica.getTipoEmpresa(),

                pessoa != null
                        ? pessoa.getId()
                        : null,

                pessoa != null
                        ? pessoa.getNome()
                        : null,

                dadosPessoaFisica != null
                        ? dadosPessoaFisica.getCpf()
                        : null
        );
    }
}