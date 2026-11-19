package br.com.cc.pessoas.unificacao.logradouro.logrDto;

public record LogradouroBaseCorreiosResultadoDTO(
        Long unificadoApagados,
        Long controleApagados,
        Long logradourosInseridos,
        Long chavesCalculadas,
        String mensagem
) {
}
