package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhCidade;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhDistrito;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhLogradouro;

public record RhLogradouroDTO(
        Long cidade,
        String cidadeNome,
        String uf,
        Long distrito,
        String distritoNome,
        Long logradouro,
        String tipoLogradouro,
        String nome
) {
    public static RhLogradouroDTO fromEntity(RhLogradouro entity) {
        RhDistrito distrito = entity.getRhDistrito();
        RhCidade cidade = distrito != null ? distrito.getRhCidade() : null;

        return new RhLogradouroDTO(
                entity.getCidade(),
                cidade != null ? cidade.getNome() : null,
                cidade != null && cidade.getEstado() != null ? cidade.getEstado().getEstado() : null,
                entity.getDistrito(),
                distrito != null ? distrito.getNome() : null,
                entity.getLogradouro(),
                entity.getTipoLogradouro().getTipoLogradouro(),
                entity.getNome()
        );
    }
}