package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoDocumento;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhTipoDocumento;

public record RhTipoDocumentoDTO(
        Long tipoDocumento,
        String descricao
) {
    public static RhTipoDocumentoDTO fromEntity(RhTipoDocumento entity) {
        return new RhTipoDocumentoDTO(
                entity.getTipoDocumento(),
                entity.getDescricao()
        );
    }
}
