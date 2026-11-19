package br.com.cc.pessoas.unificacao.cidade.cidDto;

import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;

public record OrigemCidadeDTO(
        String codigo,
        String descricao
) {
    public static OrigemCidadeDTO fromEnum(OrigemCidade origem) {
        return new OrigemCidadeDTO(
                origem.name(),
                origem.getDescricao()
        );
    }
}
