package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneTipoLogradouro;

public record SaneTipoLogradouroDTO(
        String tipoLogradouro,
        String descricao
) {
    public static SaneTipoLogradouroDTO fromEntity(SaneTipoLogradouro entity) {
        return new SaneTipoLogradouroDTO(
                entity.getTipoLogradouro(),
                entity.getDescricao()
        );
    }
}
