package br.com.cc.pessoas.unificacao.logradouro.logrDto;

/*
 * Registro de DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO candidato
 * a receber o código do logradouro de origem.
 *
 * codigoOrigemAtual maior que zero: o logradouro unificado já possui um
 * código desta origem; o vínculo vira registro duplicado (controle D).
 *
 * faixa: o complemento dos Correios indica faixa de numeração ou lado.
 *
 * criterio: NOME, TIPO_DIFERENTE, PESQUISA ou DISTRITO (todos do distrito).
 */
public record LogradouroCandidatoDTO(
        Long id,
        Long cidadeCorreios,
        Long distritoCorreios,
        Long logradouroCorreios,
        String tipo,
        String nome,
        String complemento,
        String nomeBairro,
        Boolean faixa,
        Long codigoOrigemAtual,
        String statusCor,
        String criterio
) {
}
