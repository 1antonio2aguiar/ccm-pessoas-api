package br.com.cc.pessoas.unificacao.distrito.distDto;

/*
 * Distrito do sistema de origem ainda não unificado,
 * com a sugestão de vínculo calculada pela API.
 *
 * situacao:
 *   CIDADE_NAO_UNIFICADA - a cidade do distrito ainda não está em
 *                          CIDADES_UNIFICADO (ou está em mais de uma);
 *                          unifique a cidade primeiro;
 *   UNICA                - um candidato, ainda sem código desta origem;
 *   DUPLICADA            - um candidato, mas ele já possui outro código desta origem;
 *   AMBIGUA              - mais de um candidato;
 *   SEM_CORRESPONDENCIA  - nenhum distrito com o mesmo nome na cidade.
 */
public record DistritoOrigemDTO(
        String origem,
        Long cidadeOrigem,
        Long distritoOrigem,
        String nome,
        String uf,
        String nomeCidade,
        Long cidadeCorreios,
        Long qtdCandidatos,
        String situacao,
        Long idSugerido,
        String nomeSugerido,
        Long distritoCorreiosSugerido
) {
}
