package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhPais;

public record RhPaisDTO(
        Long pais,
        String nome,
        String nacionalidade
) {
    public static RhPaisDTO fromEntity(RhPais entity) {
        return new RhPaisDTO(
                entity.getPais(),
                entity.getNome(),
                entity.getNacionalidade()
        );
    }
}
