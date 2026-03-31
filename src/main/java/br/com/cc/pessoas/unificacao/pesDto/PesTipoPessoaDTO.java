package br.com.cc.pessoas.unificacao.pesDto;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoPessoa;

public record PesTipoPessoaDTO(
        Integer tipoPessoa,
        String descricao
) {
    public static PesTipoPessoaDTO fromEntity(PesTipoPessoa entity) {
        return new PesTipoPessoaDTO(
                entity.getTipoPessoa(),
                entity.getDescricao()
        );
    }
}
