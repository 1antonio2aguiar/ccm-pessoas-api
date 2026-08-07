package br.com.cc.pessoas.unificacao.pessoa.pesDto;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesEstado;

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
