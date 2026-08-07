package br.com.cc.pessoas.unificacao.pessoa.pesDto;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesCidade;

public record PesCidadeDTO(
        Long cidade,
        String nome,
        String uf

) {
    public static PesCidadeDTO fromEntity(PesCidade entity) {
        return new PesCidadeDTO(
                entity.getCidade(),
                entity.getNome(),
                entity.getEstado().getEstado()
        );
    }
}
