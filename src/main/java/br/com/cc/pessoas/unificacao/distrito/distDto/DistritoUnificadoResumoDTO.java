package br.com.cc.pessoas.unificacao.distrito.distDto;

public record DistritoUnificadoResumoDTO(
        String origem,
        String descricao,
        Long totalOrigem,
        Long totalVinculados,
        Long totalDuplicadosRegistrados,
        Long totalNaoVincular,
        Long totalPendentes,
        Long totalCidadeNaoUnificada,
        Long totalUnicos,
        Long totalDuplicados,
        Long totalAmbiguos,
        Long totalSemCorrespondencia
) {
}
