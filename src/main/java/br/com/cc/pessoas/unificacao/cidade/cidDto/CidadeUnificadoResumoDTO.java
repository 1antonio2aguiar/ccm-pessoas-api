package br.com.cc.pessoas.unificacao.cidade.cidDto;

public record CidadeUnificadoResumoDTO(
        String origem,
        String descricao,
        Long totalOrigem,
        Long totalVinculadas,
        Long totalDuplicadasRegistradas,
        Long totalNaoVincular,
        Long totalPendentes,
        Long totalUnicas,
        Long totalDuplicadas,
        Long totalAmbiguas,
        Long totalSemCorrespondencia
) {
}
