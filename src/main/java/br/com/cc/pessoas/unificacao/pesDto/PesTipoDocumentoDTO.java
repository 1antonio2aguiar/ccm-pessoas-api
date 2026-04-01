package br.com.cc.pessoas.unificacao.pesDto;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoDocumento;

public record PesTipoDocumentoDTO(
        Long tipoDocumento,
        String descricao
) {
    public static PesTipoDocumentoDTO fromEntity(PesTipoDocumento entity) {
        return new PesTipoDocumentoDTO(
                entity.getTipoDocumento(),
                entity.getDescricao()
        );
    }
}
