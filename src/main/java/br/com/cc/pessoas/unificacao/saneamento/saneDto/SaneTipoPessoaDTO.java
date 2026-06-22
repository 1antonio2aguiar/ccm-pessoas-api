package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneTipoPessoa;

public record SaneTipoPessoaDTO(
        Integer tipoPessoa,
        String descricao
) {
    public static SaneTipoPessoaDTO fromEntity(SaneTipoPessoa entity) {
        return new SaneTipoPessoaDTO(
                entity.getTipoPessoa(),
                entity.getDescricao()
        );
    }
}
