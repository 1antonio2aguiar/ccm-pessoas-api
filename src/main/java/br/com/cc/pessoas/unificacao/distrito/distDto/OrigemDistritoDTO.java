package br.com.cc.pessoas.unificacao.distrito.distDto;

import br.com.cc.pessoas.unificacao.distrito.distEnum.OrigemDistrito;

public record OrigemDistritoDTO(
        String codigo,
        String descricao
) {
    public static OrigemDistritoDTO fromEnum(OrigemDistrito origem) {
        return new OrigemDistritoDTO(
                origem.name(),
                origem.getDescricao()
        );
    }
}
