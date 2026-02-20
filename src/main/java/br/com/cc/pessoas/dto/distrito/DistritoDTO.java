package br.com.cc.pessoas.dto.distrito;

import br.com.cc.pessoas.entity.Distrito;

public record DistritoDTO(
        Long id,
        Long cidadeId,
        String cidadeNome,
        Long estadoId,
        String estadoNome,
        String nome,
        Long codigoInep
) {

    public static DistritoDTO fromDistrito(Distrito distrito) {
        if (distrito == null) return null;

        return new DistritoDTO(
                distrito.getId(),
                distrito.getCidade().getId(),
                distrito.getCidade().getNome(),
                distrito.getCidade().getEstado().getId(),
                distrito.getCidade().getEstado().getNome(),
                distrito.getNome(),
                distrito.getCodigoInep()
        );
    }
}
