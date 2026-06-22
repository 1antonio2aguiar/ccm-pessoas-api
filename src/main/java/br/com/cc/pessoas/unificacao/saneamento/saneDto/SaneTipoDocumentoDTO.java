package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.entity.enuns.TipoDocumento;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhTipoDocumento;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SaneTipoDocumento;

public record SaneTipoDocumentoDTO(
        Long tipoDocumento,
        String descricao
) {
    public static SaneTipoDocumentoDTO fromEntity(SaneTipoDocumento entity) {
        return new SaneTipoDocumentoDTO(
                entity.getTipoDocumento(),
                entity.getDescricao()
        );
    }
}
