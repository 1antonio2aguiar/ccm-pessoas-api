package br.com.cc.pessoas.dto.pessoa;

import br.com.cc.pessoas.entity.DadosPessoaJuridica;

public record DadosPessoaJuridicaDTO(

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
        Long tipoEmpresa

) {

    public static DadosPessoaJuridicaDTO fromEntity(DadosPessoaJuridica pj) {

        if (pj == null) {
            return null;
        }

        return new DadosPessoaJuridicaDTO(
                pj.getId(),
                pj.getCnpj(),
                pj.getNome(),
                pj.getEstabelecimento(),
                pj.getNomeFantasia(),
                pj.getObjetoSocial(),
                pj.getMicroEmpresa(),
                pj.getConjuge(),
                pj.getMesEnvioSicom(),
                pj.getAnoEnvioSicom(),
                pj.getTipoEmpresa()
        );
    }
}