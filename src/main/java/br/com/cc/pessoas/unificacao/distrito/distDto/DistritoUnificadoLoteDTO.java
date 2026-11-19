package br.com.cc.pessoas.unificacao.distrito.distDto;

import java.util.List;

public record DistritoUnificadoLoteDTO(
        Long totalAnalisados,
        Long totalVinculados,
        Long totalDuplicados,
        Long totalErros,
        List<String> erros
) {
}
