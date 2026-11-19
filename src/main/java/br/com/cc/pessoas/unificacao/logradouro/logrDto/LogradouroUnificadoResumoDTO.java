package br.com.cc.pessoas.unificacao.logradouro.logrDto;

public record LogradouroUnificadoResumoDTO(
        String origem,
        String descricao,
        Long totalOrigem,
        Long totalVinculados,
        Long totalDuplicadosRegistrados,
        Long totalNaoVincular,
        Long totalPendentes,
        Long totalDistritoNaoUnificado,
        Long totalUnicos,
        Long totalFaixas,
        Long totalDuplicados,
        Long totalTipoDiferente,
        Long totalAmbiguos,
        Long totalSemCorrespondencia
) {
}
