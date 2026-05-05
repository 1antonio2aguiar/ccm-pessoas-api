package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhTipoLogradouro;

public record RhTipoLogradouroDTO(
        String tipoLogradouro,
        String descricao
) {
    public static RhTipoLogradouroDTO fromEntity(RhTipoLogradouro entity) {
        return new RhTipoLogradouroDTO(
                entity.getTipoLogradouro(),
                entity.getDescricao()
        );
    }
}
