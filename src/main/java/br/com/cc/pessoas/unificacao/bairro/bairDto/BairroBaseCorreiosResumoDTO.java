package br.com.cc.pessoas.unificacao.bairro.bairDto;

public record BairroBaseCorreiosResumoDTO(
        Long totalCorreios,
        Long totalCorreiosFicticios,
        Long totalUnificado,
        Long totalFaltando,
        Boolean permiteZerar
) {
}
