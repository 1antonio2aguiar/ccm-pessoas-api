package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhBairro;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhCidade;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhDistrito;

import java.time.LocalDate;

public record RhBairroDTO(
        Long cidade,
        String cidadeNome,
        Long distrito,
        String distritoNome,
        Long bairro,
        String nome,
        Long cidadeMacroBairro,
        Long distritoMacroBairro,
        Long macroBairro,
        String zonaRural

) {
    public static RhBairroDTO fromEntity(RhBairro entity) {
        RhDistrito distrito = entity.getRhDistrito();
        RhCidade cidade = distrito != null ? distrito.getRhCidade() : null;

        return new RhBairroDTO(
                entity.getCidade(),
                cidade != null ? cidade.getNome() : null,
                entity.getDistrito(),
                distrito != null ? distrito.getNome() : null,
                entity.getBairro(),
                entity.getNome(),
                entity.getCidadeMacroBairro(),
                entity.getDistritoMacroBairro(),
                entity.getMacroBairro(),
                entity.getZonaRural()
        );
    }
}