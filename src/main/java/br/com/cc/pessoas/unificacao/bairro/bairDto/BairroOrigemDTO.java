package br.com.cc.pessoas.unificacao.bairro.bairDto;

/*
 * Bairro do sistema de origem ainda não unificado,
 * com a sugestão de vínculo calculada pela API.
 *
 * situacao:
 *   DISTRITO_NAO_UNIFICADO - cidade/distrito do bairro ainda não estão em
 *                            DISTRITOS_UNIFICADO (ou estão em mais de um);
 *                            unifique o distrito primeiro;
 *   UNICA                  - um candidato, ainda sem código desta origem;
 *   DUPLICADA              - um candidato, mas ele já possui outro código desta origem;
 *   AMBIGUA                - mais de um candidato;
 *   SEM_CORRESPONDENCIA    - nenhum bairro com o mesmo nome no distrito.
 */
public record BairroOrigemDTO(
        String origem,
        Long cidadeOrigem,
        Long distritoOrigem,
        Long bairroOrigem,
        String nome,
        String uf,
        String nomeCidade,
        Long cidadeCorreios,
        Long distritoCorreios,
        Long qtdCandidatos,
        String situacao,
        Long idSugerido,
        String nomeSugerido,
        Long bairroCorreiosSugerido
) {
}
