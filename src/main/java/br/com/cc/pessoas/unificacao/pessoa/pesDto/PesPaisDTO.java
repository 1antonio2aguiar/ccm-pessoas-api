package br.com.cc.pessoas.unificacao.pessoa.pesDto;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPais;

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
