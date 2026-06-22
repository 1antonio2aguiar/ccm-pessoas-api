package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SanePais;

public record SanePaisDTO(
        Long pais,
        String nome,
        String nacionalidade
) {
    public static SanePaisDTO fromEntity(SanePais entity) {
        return new SanePaisDTO(
                entity.getPais(),
                entity.getNome(),
                entity.getNacionalidade()
        );
    }
}
