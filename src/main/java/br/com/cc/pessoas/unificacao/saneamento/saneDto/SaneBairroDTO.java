package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneBairro;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneCidade;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneDistrito;

public record SaneBairroDTO(
        Long cidade,
        String cidadeNome,
        Long distrito,
        String distritoNome,
        Long bairro,
        String nome

) {
    public static SaneBairroDTO fromEntity(SaneBairro entity) {
        SaneDistrito distrito = entity.getSaneDistrito();
        SaneCidade cidade = distrito != null ? distrito.getSaneCidade() : null;

        return new SaneBairroDTO(
                entity.getCidade(),
                cidade != null ? cidade.getNome() : null,
                entity.getDistrito(),
                distrito != null ? distrito.getNome() : null,
                entity.getBairro(),
                entity.getNome()
        );
    }
}