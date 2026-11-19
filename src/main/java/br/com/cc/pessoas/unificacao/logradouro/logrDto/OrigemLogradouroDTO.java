package br.com.cc.pessoas.unificacao.logradouro.logrDto;

import br.com.cc.pessoas.unificacao.logradouro.logrEnum.OrigemLogradouro;

public record OrigemLogradouroDTO(
        String codigo,
        String descricao
) {
    public static OrigemLogradouroDTO fromEnum(OrigemLogradouro origem) {
        return new OrigemLogradouroDTO(origem.name(), origem.getDescricao());
    }
}
