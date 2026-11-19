package br.com.cc.pessoas.unificacao.cidade.cidDto;

import java.util.List;

public record CidadeUnificadoLoteDTO(
        Long totalAnalisados,
        Long totalVinculados,
        Long totalDuplicados,
        Long totalErros,
        List<String> erros
) {
}
