package br.com.cc.pessoas.unificacao.logradouro.logrDto;

/*
 * Logradouro do sistema de origem ainda não unificado,
 * com a sugestão de vínculo calculada pela API.
 *
 * A comparação usa a chave do nome (PKG_LOGRADOURO_CHAVE: sem acento,
 * sem pontuação, sem preposição e com títulos abreviados por extenso),
 * sempre dentro do MESMO distrito dos Correios.
 *
 * situacao:
 *   DISTRITO_NAO_UNIFICADO - cidade/distrito do logradouro ainda não estão em
 *                            DISTRITOS_UNIFICADO (ou estão em mais de um);
 *   UNICA                  - um candidato com o mesmo nome e tipo compatível;
 *   FAIXAS                 - mesmo nome e tipo, mas dividido em faixas de numeração
 *                            pelos Correios; a sugestão é o trecho principal
 *                            (sem faixa no complemento, senão o de menor código);
 *   DUPLICADA              - o candidato sugerido já possui outro código desta origem;
 *   TIPO_DIFERENTE         - mesmo nome, mas tipo diferente (ex.: RUA x AVENIDA);
 *                            fica fora do lote, decida na tela;
 *   AMBIGUA                - mais de um candidato que não são faixas do mesmo logradouro;
 *   SEM_CORRESPONDENCIA    - nenhum logradouro com o mesmo nome no distrito.
 */
public record LogradouroOrigemDTO(
        String origem,
        Long cidadeOrigem,
        Long distritoOrigem,
        Long logradouroOrigem,
        String tipo,
        String nome,
        String uf,
        String nomeCidade,
        Long cidadeCorreios,
        Long distritoCorreios,
        Long qtdCandidatos,
        String situacao,
        Long idSugerido,
        String nomeSugerido,
        Long logradouroCorreiosSugerido,
        String complementoSugerido
) {
}
