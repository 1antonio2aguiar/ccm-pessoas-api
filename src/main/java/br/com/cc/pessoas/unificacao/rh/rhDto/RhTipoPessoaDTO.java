package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhTipoPessoa;

public record RhTipoPessoaDTO(
        Integer tipoPessoa,
        String descricao
) {
    public static RhTipoPessoaDTO fromEntity(RhTipoPessoa entity) {
        return new RhTipoPessoaDTO(
                entity.getTipoPessoa(),
                entity.getDescricao()
        );
    }
}
