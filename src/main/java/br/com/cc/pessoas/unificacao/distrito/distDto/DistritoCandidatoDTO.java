package br.com.cc.pessoas.unificacao.distrito.distDto;

/*
 * Registro de DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO candidato
 * a receber o código do distrito de origem.
 *
 * codigoOrigemAtual maior que zero indica que o distrito unificado
 * já possui um código desta origem. Neste caso o vínculo é
 * registrado como duplicado em DISTRITOS_UNIFICADO_CTRL.
 *
 * criterio: NOME, PESQUISA ou CIDADE (todos os distritos da cidade).
 */
public record DistritoCandidatoDTO(
        Long id,
        Long cidadeCorreios,
        Long distritoCorreios,
        String nome,
        String uf,
        Long cep,
        Long codigoOrigemAtual,
        String statusCor,
        String criterio
) {
}
