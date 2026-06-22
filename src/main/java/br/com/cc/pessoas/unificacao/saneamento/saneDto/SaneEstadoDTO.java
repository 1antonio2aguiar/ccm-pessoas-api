package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneEstado;

public record SaneEstadoDTO(
        String estado,
        String descricao
) {
    public static SaneEstadoDTO fromEntity(SaneEstado entity) {
        return new SaneEstadoDTO(
                entity.getEstado(),
                entity.getDescricao()
        );
    }
}
