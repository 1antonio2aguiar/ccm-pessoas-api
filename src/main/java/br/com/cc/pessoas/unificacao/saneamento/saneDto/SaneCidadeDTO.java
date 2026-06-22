package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneCidade;

public record SaneCidadeDTO(
        Long cidade,
        String nome,
        String uf,
        Long cep

) {
    public static SaneCidadeDTO fromEntity(SaneCidade entity) {
        return new SaneCidadeDTO(
                entity.getCidade(),
                entity.getNome(),
                entity.getEstado().getEstado(),
                entity.getCep()
        );
    }
}
