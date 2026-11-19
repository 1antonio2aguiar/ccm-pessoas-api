package br.com.cc.pessoas.unificacao.bairro.bairDto;

/*
 * Registro de DBO_UNI_PESSOAS.BAIRROS_UNIFICADO candidato
 * a receber o código do bairro de origem.
 *
 * codigoOrigemAtual maior que zero: o bairro unificado já possui um
 * código desta origem; o vínculo vira registro duplicado (controle D).
 *
 * criterio: NOME, PESQUISA ou DISTRITO (todos os bairros do distrito).
 */
public record BairroCandidatoDTO(
        Long id,
        Long cidadeCorreios,
        Long distritoCorreios,
        Long bairroCorreios,
        String nome,
        String nomeReduzido,
        Long codigoOrigemAtual,
        String statusCor,
        String criterio
) {
}
