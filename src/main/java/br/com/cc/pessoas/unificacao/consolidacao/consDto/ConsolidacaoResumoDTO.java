package br.com.cc.pessoas.unificacao.consolidacao.consDto;

/*
 * totalUnificado:  linhas (uma por chave dos Correios) que vão para a consolidada;
 * totalConsolidado: linhas hoje na tabela consolidada;
 * totalFaltando:   chaves do unificado que ainda não estão na consolidada;
 * totalSoConsolidado: linhas da consolidada sem correspondente no unificado
 *                     (cargas antigas; nunca são apagadas, só informadas).
 */
public record ConsolidacaoResumoDTO(
        String nivel,
        String descricao,
        String tabela,
        Long totalUnificado,
        Long totalConsolidado,
        Long totalFaltando,
        Long totalSoConsolidado
) {
}
