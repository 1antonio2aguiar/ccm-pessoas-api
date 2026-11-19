package br.com.cc.pessoas.unificacao.distrito.distDto;

/*
 * Situação da base dos Correios em DISTRITOS_UNIFICADO.
 *
 * totalFaltando: distritos de DBO_CORREIOS.DISTRITOS (com cidade)
 * que ainda não possuem linha em DISTRITOS_UNIFICADO.
 */
public record DistritoBaseCorreiosResumoDTO(
        Long totalCorreios,
        Long totalCorreiosFicticios,
        Long totalUnificado,
        Long totalFaltando,
        Boolean permiteZerar
) {
}
