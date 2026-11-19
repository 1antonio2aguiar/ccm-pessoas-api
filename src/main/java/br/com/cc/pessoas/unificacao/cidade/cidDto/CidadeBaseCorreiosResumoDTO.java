package br.com.cc.pessoas.unificacao.cidade.cidDto;

/*
 * Situação da base dos Correios em CIDADES_UNIFICADO.
 *
 * totalFaltando: cidades de DBO_CORREIOS.CIDADES que ainda
 * não possuem linha em CIDADES_UNIFICADO.
 */
public record CidadeBaseCorreiosResumoDTO(
        Long totalCorreios,
        Long totalCorreiosFicticias,
        Long totalUnificado,
        Long totalFaltando,
        Boolean permiteZerar
) {
}
