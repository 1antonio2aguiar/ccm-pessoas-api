package br.com.cc.pessoas.unificacao.pesDto;

import br.com.cc.pessoas.unificacao.pesEntity.PesEstado;

public record PesEstadoDTO(
        String estado,
        String descricao
) {
    public static PesEstadoDTO fromEntity(PesEstado entity) {
        return new PesEstadoDTO(
                entity.getEstado(),
                entity.getDescricao()
        );
    }
}
