package br.com.cc.pessoas.dto.tipoLogradouro;

import br.com.cc.pessoas.entity.TipoLogradouro;

public record TipoLogradouroDTO(
        Long id,
        String descricao,
        String sigla
) {
    public static TipoLogradouroDTO fromEntity(TipoLogradouro entity) {
        return new TipoLogradouroDTO(
                entity.getId(),
                entity.getDescricao(),
                entity.getSigla()
        );
    }
}
