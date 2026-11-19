package br.com.cc.pessoas.unificacao.logradouro.logrDto;

import java.util.List;

public record LogradouroUnificadoLoteDTO(
        Long totalAnalisados,
        Long totalVinculados,
        Long totalDuplicados,
        Long totalErros,
        List<String> erros
) {
}
