package br.com.cc.pessoas.unificacao.pessoa.pesDto;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesDistrito;

public record PesDistritoDTO(
        Long cidade,
        String cidadeNome,
        String uf,
        Long distrito,
        String nome
) {
    public static PesDistritoDTO fromEntity(PesDistrito entity) {

        return new PesDistritoDTO(
                entity.getPesCidade().getCidade(),
                entity.getPesCidade().getNome(),
                entity.getPesCidade().getEstado().getEstado(),
                entity.getDistrito(),
                entity.getNome()
        );
    }
}