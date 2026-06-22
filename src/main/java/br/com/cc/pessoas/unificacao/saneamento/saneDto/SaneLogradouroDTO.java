package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.entity.Cidade;
import br.com.cc.pessoas.entity.Distrito;
import br.com.cc.pessoas.entity.Logradouro;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhCidade;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhDistrito;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhLogradouro;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneCidade;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneDistrito;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneLogradouro;

public record SaneLogradouroDTO(
        Long cidade,
        String cidadeNome,
        String uf,
        Long distrito,
        String distritoNome,
        Long logradouro,
        String tipoLogradouro,
        String nome
) {
    public static SaneLogradouroDTO fromEntity(SaneLogradouro entity) {
        SaneDistrito distrito = entity.getSaneDistrito();
        SaneCidade cidade = distrito != null ? distrito.getSaneCidade() : null;

        return new SaneLogradouroDTO(
                entity.getCidade(),
                cidade != null ? cidade.getNome() : null,
                cidade != null && cidade.getEstado() != null ? cidade.getEstado().getEstado() : null,
                entity.getDistrito(),
                distrito != null ? distrito.getNome() : null,
                entity.getLogradouro(),
                entity.getTipoLogradouro().getTipoLogradouro(),
                entity.getNome()
        );
    }
}