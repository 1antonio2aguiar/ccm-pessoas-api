package br.com.cc.pessoas.unificacao.cargafinal.cargDto;

import br.com.cc.pessoas.unificacao.cargafinal.cargEnum.NivelCargaFinal;

public record NivelCargaFinalDTO(
        String codigo,
        String descricao
) {
    public static NivelCargaFinalDTO fromEnum(NivelCargaFinal nivel) {
        return new NivelCargaFinalDTO(nivel.name(), nivel.getDescricao());
    }
}
