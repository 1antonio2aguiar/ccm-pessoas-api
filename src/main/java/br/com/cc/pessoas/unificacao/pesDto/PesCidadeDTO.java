package br.com.cc.pessoas.unificacao.pesDto;

import br.com.cc.pessoas.unificacao.pesEntity.PesCidade;
import br.com.cc.pessoas.unificacao.pesEntity.PesPais;

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
