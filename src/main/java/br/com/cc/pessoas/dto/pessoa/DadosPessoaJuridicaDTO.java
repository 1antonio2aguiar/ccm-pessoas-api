package br.com.cc.pessoas.dto.pessoa;

import br.com.cc.pessoas.entity.DadosPessoaJuridica;

import java.util.Optional;

public record DadosPessoaJuridicaDTO(

        String cnpj,
        String nomeFantasia,
        String objetoSocial,
        String microEmpresa,
        String conjuge,
        String tipoEmpresa

) {

    public static DadosPessoaJuridicaDTO fromEntity(DadosPessoaJuridica pj) {

        if (pj == null) {
            return null;
        }

        return new DadosPessoaJuridicaDTO(
                pj.getCnpj(),
                pj.getNomeFantasia(),
                pj.getObjetoSocial(),
                pj.getMicroEmpresa(),
                pj.getConjuge(),
                pj.getTipoEmpresa()
        );
    }

    public static DadosPessoaJuridicaDTO fromOptional(Optional<DadosPessoaJuridica> pj) {
        return pj.map(DadosPessoaJuridicaDTO::fromEntity).orElse(null);
    }
}
