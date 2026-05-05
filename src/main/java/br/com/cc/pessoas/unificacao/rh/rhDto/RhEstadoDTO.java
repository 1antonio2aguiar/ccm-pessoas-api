package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhEstado;

public record RhEstadoDTO(
        String estado,
        String descricao
) {
    public static RhEstadoDTO fromEntity(RhEstado entity) {
        return new RhEstadoDTO(
                entity.getEstado(),
                entity.getDescricao()
        );
    }
}
