package br.com.cc.pessoas.unificacao.consolidacao.consDto;

import br.com.cc.pessoas.unificacao.consolidacao.consEnum.NivelConsolidacao;

public record NivelConsolidacaoDTO(
        String codigo,
        String descricao,
        String tabela
) {
    public static NivelConsolidacaoDTO fromEnum(NivelConsolidacao nivel) {
        return new NivelConsolidacaoDTO(nivel.name(), nivel.getDescricao(), nivel.getTabelaDestino());
    }
}
