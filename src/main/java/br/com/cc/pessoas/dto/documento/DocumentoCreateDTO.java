package br.com.cc.pessoas.dto.documento;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DocumentoCreateDTO(
        @NotNull
        Integer tipoDocumento,
        @NotNull
        Long pessoaId,
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
        public String getNumeroDocumento() {
                return numeroDocumento != null ? numeroDocumento.toUpperCase() : "";
        }
        public String getObservacao() {
                return observacao != null ? observacao.toUpperCase() : "";
        }
        public String getDocumentoOrigem() {
                return documentoOrigem != null ? documentoOrigem.toUpperCase() : "";
        }
        public String getOrgaoExpedidor() {
                return orgaoExpedidor != null ? orgaoExpedidor.toUpperCase() : "";
        }
        public String getCategoriaCnh() {
                return categoriaCnh != null ? categoriaCnh.toUpperCase() : "";
        }

}
