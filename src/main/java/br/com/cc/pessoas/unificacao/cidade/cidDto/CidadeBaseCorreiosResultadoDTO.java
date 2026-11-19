package br.com.cc.pessoas.unificacao.cidade.cidDto;

public record CidadeBaseCorreiosResultadoDTO(
        Long unificadoApagadas,
        Long controleApagados,
        Long ficticiasApagadas,
        Long cidadesInseridas,
        String mensagem
) {
}
