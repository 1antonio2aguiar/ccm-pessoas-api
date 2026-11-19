package br.com.cc.pessoas.unificacao.consolidacao.consDto;

public record ConsolidacaoResultadoDTO(
        String nivel,
        String descricao,
        Long inseridos,
        Long completados,
        Boolean sucesso,
        String mensagem
) {
}
