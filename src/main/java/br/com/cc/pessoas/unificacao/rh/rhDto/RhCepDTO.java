package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhCep;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhCidade;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhDistrito;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhLogradouro;

public record RhCepDTO(
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
    public static RhCepDTO fromEntity(RhCep entity) {
        RhLogradouro logradouro = entity.getRhLogradouro();
        RhDistrito distrito = logradouro != null ? logradouro.getRhDistrito() : null;
        RhCidade cidade = distrito != null ? distrito.getRhCidade() : null;

        return new RhCepDTO(
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