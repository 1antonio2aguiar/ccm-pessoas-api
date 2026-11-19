package br.com.cc.pessoas.unificacao.bairro.bairDto;

public record BairroUnificadoResumoDTO(
        String origem,
        String descricao,
        Long totalOrigem,
        Long totalVinculados,
        Long totalDuplicadosRegistrados,
        Long totalNaoVincular,
        Long totalPendentes,
        Long totalDistritoNaoUnificado,
        Long totalUnicos,
        Long totalDuplicados,
        Long totalAmbiguos,
        Long totalSemCorrespondencia
) {
}
