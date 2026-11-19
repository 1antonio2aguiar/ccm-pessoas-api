package br.com.cc.pessoas.unificacao.cargafinal.cargDto;

public record CargaFinalResultadoDTO(
        String nivel,
        String descricao,
        Long codigosNovos,
        Long codigosReaproveitados,
        Long inseridosCcEnderecos,
        Long inseridosCcmPessoas,
        Boolean sucesso,
        String mensagem
) {
}
