package br.com.cc.pessoas.unificacao.logradouro.logrDto;

public record LogradouroBaseCorreiosResumoDTO(
        Long totalCorreios,
        Long totalCorreiosFicticios,
        Long totalUnificado,
        Long totalFaltando,
        Long totalSemChave,
        Boolean permiteZerar
) {
}
