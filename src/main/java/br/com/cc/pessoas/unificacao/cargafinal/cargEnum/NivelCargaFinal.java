package br.com.cc.pessoas.unificacao.cargafinal.cargEnum;

import br.com.cc.pessoas.service.exceptions.NegocioException;

/*
 * Níveis da carga final (fase 3): das tabelas consolidadas de DBO_UNI_PESSOAS
 * para as tabelas usadas pelos sistemas, em DBO_CC_ENDERECOS e DBO_CCM_PESSOAS
 * (mesmo ID nos dois schemas, como nas procedures).
 *
 * Substitui SP_CARGA_ENDERECOS e SP_CARGA_CEPS. A ordem importa: cada nível
 * só carrega o que tem o nível de cima já carregado.
 */
public enum NivelCargaFinal {

    CIDADES("Cidades"),
    DISTRITOS("Distritos"),
    BAIRROS("Bairros"),
    LOGRADOUROS("Logradouros"),
    CEPS("CEPs");

    private final String descricao;

    NivelCargaFinal(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static NivelCargaFinal from(String valor) {

        if (valor == null || valor.isBlank()) {
            throw new NegocioException("Nível não informado.");
        }

        try {
            return NivelCargaFinal.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Nível inválido: " + valor + ".");
        }
    }
}
