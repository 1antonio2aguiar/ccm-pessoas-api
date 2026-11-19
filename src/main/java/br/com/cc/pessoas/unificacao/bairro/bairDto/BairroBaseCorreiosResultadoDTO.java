package br.com.cc.pessoas.unificacao.bairro.bairDto;

public record BairroBaseCorreiosResultadoDTO(
        Long unificadoApagados,
        Long controleApagados,
        Long bairrosInseridos,
        String mensagem
) {
}
