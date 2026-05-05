package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.entity.Cidade;
import br.com.cc.pessoas.unificacao.pesEntity.PesCidade;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhCidade;

public record RhCidadeDTO(
        Long cidade,
        String nome,
        String uf

) {
    public static RhCidadeDTO fromEntity(RhCidade entity) {
        return new RhCidadeDTO(
                entity.getCidade(),
                entity.getNome(),
                entity.getEstado().getEstado()
        );
    }
}
