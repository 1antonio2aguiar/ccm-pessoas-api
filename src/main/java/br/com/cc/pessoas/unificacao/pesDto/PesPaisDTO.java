package br.com.cc.pessoas.unificacao.pesDto;

import br.com.cc.pessoas.unificacao.pesEntity.PesPais;

public record PesPaisDTO(
        Long pais,
        String nome,
        String nacionalidade
) {
    public static PesPaisDTO fromEntity(PesPais entity) {
        return new PesPaisDTO(
                entity.getPais(),
                entity.getNome(),
                entity.getNacionalidade()
        );
    }
}
