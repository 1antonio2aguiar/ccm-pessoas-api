package br.com.cc.pessoas.unificacao.bairro.bairDto;

import br.com.cc.pessoas.unificacao.bairro.bairEnum.OrigemBairro;

public record OrigemBairroDTO(
        String codigo,
        String descricao
) {
    public static OrigemBairroDTO fromEnum(OrigemBairro origem) {
        return new OrigemBairroDTO(origem.name(), origem.getDescricao());
    }
}
