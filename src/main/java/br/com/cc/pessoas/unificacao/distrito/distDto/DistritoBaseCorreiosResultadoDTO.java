package br.com.cc.pessoas.unificacao.distrito.distDto;

public record DistritoBaseCorreiosResultadoDTO(
        Long unificadoApagados,
        Long controleApagados,
        Long distritosInseridos,
        String mensagem
) {
}
