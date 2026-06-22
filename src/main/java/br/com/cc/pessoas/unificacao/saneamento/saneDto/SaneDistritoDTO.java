package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.entity.Distrito;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhDistrito;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneDistrito;

public record SaneDistritoDTO(
        Long cidade,
        String cidadeNome,
        String uf,
        Long distrito,
        String nome
) {
    public static SaneDistritoDTO fromEntity(SaneDistrito entity) {

        return new SaneDistritoDTO(
                entity.getCidade(),
                entity.getNome(),
                entity.getSaneCidade().getEstado().getEstado(),
                entity.getDistrito(),
                entity.getNome()
        );
    }
}