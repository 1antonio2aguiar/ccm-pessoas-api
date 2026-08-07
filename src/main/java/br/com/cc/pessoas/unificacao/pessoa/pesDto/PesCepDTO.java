package br.com.cc.pessoas.unificacao.pessoa.pesDto;

import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesCep;
import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesCidade;
import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesDistrito;
import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesLogradouro;

public record PesCepDTO(
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
    public static PesCepDTO fromEntity(PesCep entity) {
        PesLogradouro logradouro = entity.getPesLogradouro();
        PesDistrito distrito = logradouro != null ? logradouro.getPesDistrito() : null;
        PesCidade cidade = distrito != null ? distrito.getPesCidade() : null;

        return new PesCepDTO(
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