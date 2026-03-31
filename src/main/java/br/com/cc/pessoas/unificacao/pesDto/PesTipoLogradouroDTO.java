package br.com.cc.pessoas.unificacao.pesDto;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoLogradouro;

public record PesTipoLogradouroDTO(
        String tipoLogradouro,
        String descricao
) {
    public static PesTipoLogradouroDTO fromEntity(PesTipoLogradouro entity) {
        return new PesTipoLogradouroDTO(
                entity.getTipoLogradouro(),
                entity.getDescricao()
        );
    }
}
