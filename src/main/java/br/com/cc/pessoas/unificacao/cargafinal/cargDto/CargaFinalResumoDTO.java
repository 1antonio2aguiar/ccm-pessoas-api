package br.com.cc.pessoas.unificacao.cargafinal.cargDto;

/*
 * totalConsolidado:   linhas na tabela consolidada de DBO_UNI_PESSOAS;
 * comCodigo:          já têm CODIGO_CCM (cidades: o próprio código);
 * pendentes:          sem CODIGO_CCM e prontos para carregar;
 * bloqueados:         sem CODIGO_CCM, mas o nível de cima ainda não foi
 *                     carregado (ou, em cidades, a UF não existe em ESTADOS;
 *                     em CEPs, logradouro ou bairro sem código);
 * faltandoCcEnderecos / faltandoCcmPessoas: têm código, mas a linha não existe
 *                     na tabela final (a carga também completa esses).
 */
public record CargaFinalResumoDTO(
        String nivel,
        String descricao,
        Long totalConsolidado,
        Long comCodigo,
        Long pendentes,
        Long bloqueados,
        Long faltandoCcEnderecos,
        Long faltandoCcmPessoas
) {
}
