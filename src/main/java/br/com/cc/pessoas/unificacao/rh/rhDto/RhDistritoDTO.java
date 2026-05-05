package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhDistrito;

public record RhDistritoDTO(
        Long cidade,
        String cidadeNome,
        String uf,
        Long distrito,
        String nome
) {
    public static RhDistritoDTO fromEntity(RhDistrito entity) {

        return new RhDistritoDTO(
                entity.getRhCidade().getCidade(),
                entity.getRhCidade().getNome(),
                entity.getRhCidade().getEstado().getEstado(),
                entity.getDistrito(),
                entity.getNome()
        );
    }
}