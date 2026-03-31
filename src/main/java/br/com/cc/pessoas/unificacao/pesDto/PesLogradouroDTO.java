package br.com.cc.pessoas.unificacao.pesDto;

import br.com.cc.pessoas.unificacao.pesEntity.PesCidade;
import br.com.cc.pessoas.unificacao.pesEntity.PesDistrito;
import br.com.cc.pessoas.unificacao.pesEntity.PesLogradouro;

public record PesLogradouroDTO(
        Long cidade,
        String cidadeNome,
        String uf,
        Long distrito,
        String distritoNome,
        Long logradouro,
        String tipoLogradouro,
        String nome,
        String nomeLegal
) {
    public static PesLogradouroDTO fromEntity(PesLogradouro entity) {
        PesDistrito distrito = entity.getPesDistrito();
        PesCidade cidade = distrito != null ? distrito.getPesCidade() : null;

        return new PesLogradouroDTO(
                entity.getCidade(),
                cidade != null ? cidade.getNome() : null,
                cidade != null && cidade.getEstado() != null ? cidade.getEstado().getEstado() : null,
                entity.getDistrito(),
                distrito != null ? distrito.getNome() : null,
                entity.getLogradouro(),
                entity.getTipoLogradouro().getTipoLogradouro(),
                entity.getNome(),
                entity.getNomeLegal()
        );
    }
}