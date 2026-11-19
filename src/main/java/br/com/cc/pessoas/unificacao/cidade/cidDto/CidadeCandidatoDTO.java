package br.com.cc.pessoas.unificacao.cidade.cidDto;

/*
 * Registro de DBO_UNI_PESSOAS.CIDADES_UNIFICADO
 * candidato a receber o código da cidade de origem.
 *
 * codigoOrigemAtual maior que zero indica que a cidade unificada
 * já possui um código desta origem. Neste caso o vínculo é
 * registrado como duplicado em CIDADES_UNIFICADO_CTRL.
 *
 * criterio: NOME, IBGE ou PESQUISA (busca manual do usuário).
 */
public record CidadeCandidatoDTO(
        Long id,
        Long cidadeCorreios,
        String nome,
        String uf,
        Long cep,
        Long codigoIbge,
        Long codigoOrigemAtual,
        String statusCor,
        String criterio
) {
}
