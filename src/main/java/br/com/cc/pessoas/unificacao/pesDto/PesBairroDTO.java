package br.com.cc.pessoas.unificacao.pesDto;

import br.com.cc.pessoas.unificacao.pesEntity.PesBairro;
import br.com.cc.pessoas.unificacao.pesEntity.PesCidade;
import br.com.cc.pessoas.unificacao.pesEntity.PesDistrito;

import java.time.LocalDate;

public record PesBairroDTO(
        Long cidade,
        String cidadeNome,
        Long distrito,
        String distritoNome,
        Long bairro,
        String nome,
        Long cidadeMacroBairro,
        Long distritoMacroBairro,
        Long macroBairro,
        LocalDate dataCriacao,
        String leiCriacao,
        LocalDate dataDecreto,
        Long decreto,
        LocalDate dataPortaria,
        Long portaria,
        String nomeVereador,
        Long pessoaLoteadora,
        String tipoBairro
) {
    public static PesBairroDTO fromEntity(PesBairro entity) {
        PesDistrito distrito = entity.getPesDistrito();
        PesCidade cidade = distrito != null ? distrito.getPesCidade() : null;

        return new PesBairroDTO(
                entity.getCidade(),
                cidade != null ? cidade.getNome() : null,
                entity.getDistrito(),
                distrito != null ? distrito.getNome() : null,
                entity.getBairro(),
                entity.getNome(),
                entity.getCidadeMacroBairro(),
                entity.getDistritoMacroBairro(),
                entity.getMacroBairro(),
                entity.getDataCriacao(),
                entity.getLeiCriacao(),
                entity.getDataDecreto(),
                entity.getDecreto(),
                entity.getDataPortaria(),
                entity.getPortaria(),
                entity.getNomeVereador(),
                entity.getPessoaLoteadora(),
                entity.getTipoBairro()
        );
    }
}