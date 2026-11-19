package br.com.cc.pessoas.unificacao.bairro.bairDto;

import java.util.List;

public record BairroUnificadoLoteDTO(
        Long totalAnalisados,
        Long totalVinculados,
        Long totalDuplicados,
        Long totalErros,
        List<String> erros
) {
}
