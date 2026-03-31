package br.com.cc.pessoas.dto.documento;

import br.com.cc.pessoas.entity.Documento;

import java.time.LocalDate;

public record DocumentoDTO(
        Long id,
        Long pessoaId,
        String pessoaNome,
        Integer tipoDocumento, // codigo do Enum
        String tipoDocumentoDescricao, // Descrição do Enum
        String numeroDocumento,
        LocalDate dataDocumento,
        LocalDate dataExpedicao,
        String documentoOrigem,
        String orgaoExpedidor,
        LocalDate dataPrimeiraCnh,
        LocalDate dataValidade,
        String categoriaCnh,
        Long zona,
        Long secao,
        String observacao

) {

    public static DocumentoDTO fromDocumento(Documento documento) {
        if (documento == null) return null;

        return new DocumentoDTO(
                documento.getId(),
                documento.getPessoa().getId(),
                documento.getPessoa().getNome(),
                documento.getTipoDocumento() != null ? documento.getTipoDocumento().getCodigo() : null,
                documento.getTipoDocumento() != null ? documento.getTipoDocumento().getDescricao() : null,
                documento.getNumeroDocumento(),
                documento.getDataDocumento(),
                documento.getDataExpedicao(),
                documento.getDocumentoOrigem(),
                documento.getOrgaoExpedidor(),
                documento.getDataPrimeiraCnh(),
                documento.getDataValidade(),
                documento.getCategoriaCnh(),
                documento.getZona(),
                documento.getSecao(),
                documento.getObservacao()
        );
    }
}
