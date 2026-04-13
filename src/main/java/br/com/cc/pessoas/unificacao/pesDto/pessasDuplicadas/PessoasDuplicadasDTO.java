package br.com.cc.pessoas.unificacao.pesDto.pessasDuplicadas;

import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.Pessoasduplicadas;

import java.util.List;

public record PessoasDuplicadasDTO(
        Long id,
        String banco,
        Long cpf,
        String nome,
        List<Long> codigosPessoas
) {
    public static PessoasDuplicadasDTO fromEntity(Pessoasduplicadas entity) {
        return new PessoasDuplicadasDTO(
                entity.getId(),
                entity.getBanco(),
                entity.getCpf(),
                entity.getNome(),
                entity.getCodigosPessoasDuplicadas()
        );
    }
}