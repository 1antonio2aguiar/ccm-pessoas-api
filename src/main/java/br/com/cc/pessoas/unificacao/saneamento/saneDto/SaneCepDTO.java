package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.entity.Cep;
import br.com.cc.pessoas.entity.Cidade;
import br.com.cc.pessoas.entity.Distrito;
import br.com.cc.pessoas.entity.Logradouro;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhCep;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhCidade;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhDistrito;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhLogradouro;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneCep;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneCidade;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneDistrito;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneLogradouro;

public record SaneCepDTO(
        Long cidade,
        String cidadeNome,
        Long distrito,
        String distritoNome,
        Long logradouro,
        String logradouroNome,
        Long numeroIni,
        Long numeroFim,
        Long cep,
        String identificacao
) {
    public static SaneCepDTO fromEntity(SaneCep entity) {
        SaneLogradouro logradouro = entity.getSaneLogradouro();
        SaneDistrito distrito = logradouro != null ? logradouro.getSaneDistrito() : null;
        SaneCidade cidade = distrito != null ? distrito.getSaneCidade() : null;

        return new SaneCepDTO(
                entity.getCidade(),
                cidade != null ? cidade.getNome() : null,
                entity.getDistrito(),
                distrito != null ? distrito.getNome() : null,
                entity.getLogradouro(),
                logradouro != null ? logradouro.getNome() : null,
                entity.getNumero_ini(),
                entity.getNumero_fim(),
                entity.getCep(),
                entity.getIdentificacao()
        );
    }
}