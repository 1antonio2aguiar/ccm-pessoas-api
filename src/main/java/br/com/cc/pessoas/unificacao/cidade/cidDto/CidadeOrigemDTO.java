package br.com.cc.pessoas.unificacao.cidade.cidDto;

/*
 * Cidade do sistema de origem ainda não unificada,
 * com a sugestão de vínculo calculada pela API.
 *
 * situacao:
 *   UNICA               - um candidato, ainda sem código desta origem;
 *   DUPLICADA           - um candidato, mas ele já possui outro código desta origem;
 *   AMBIGUA             - mais de um candidato;
 *   SEM_CORRESPONDENCIA - nenhum candidato por nome/UF ou IBGE.
 */
public record CidadeOrigemDTO(
        String origem,
        Long cidadeOrigem,
        String nome,
        String uf,
        Long cep,
        String ddd,
        Long codigoIbge,
        Long qtdCandidatos,
        String situacao,
        Long idSugerido,
        String nomeSugerido,
        Long cidadeCorreiosSugerida
) {
}
