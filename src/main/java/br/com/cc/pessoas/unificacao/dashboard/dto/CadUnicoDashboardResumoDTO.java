package br.com.cc.pessoas.unificacao.dashboard.dto;

public record CadUnicoDashboardResumoDTO(
        Long totalPessoasUnificadas,
        Long totalVinculos,
        Long totalOrigemPessoas,
        Long totalOrigemRh,
        Long totalOrigemSane,
        Long totalCpf,
        Long totalCnpj,
        Long totalSemEndereco,
        Long totalComMaisDeUmaOrigem
) {}