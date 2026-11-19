package br.com.cc.pessoas.unificacao.logradouro.logrEnum;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import br.com.cc.pessoas.unificacao.distrito.distEnum.OrigemDistrito;

import java.util.List;

/*
 * Configuração de cada sistema de origem na unificação de logradouros.
 *
 * Cada origem informa:
 *
 * - a origem de distrito correspondente (OrigemDistrito), usada para achar
 *   cidade + distrito dos Correios pelo de-para de DISTRITOS_UNIFICADO;
 * - os sinônimos das tabelas de logradouros e de tipos de logradouro;
 * - as colunas CIDADE_x / DISTRITO_x / LOGRADOURO_x de LOGRADOUROS_UNIFICADO;
 * - os COMPLEMENTOS: colunas de LOGRADOUROS_UNIFICADO preenchidas com dados
 *   da origem quando estiverem vazias (nunca sobrescreve).
 *
 * O CEP da Saúde só é aproveitado quando existe na tabela de CEPs dos Correios.
 */
public enum OrigemLogradouro {

    PES(
            OrigemDistrito.PES,
            "PES_LOGRADOUROS",
            "PES_TIPOS_LOGRADOUROS",
            "CIDADE_PESSOA",
            "DISTRITO_PESSOA",
            "LOGRADOURO_PESSOA",
            List.of(
                    Complemento.outro("NOME_LEGAL", "o.nome_legal")
            )
    ),

    RH(
            OrigemDistrito.RH,
            "RH_LOGRADOUROS",
            "RH_TIPOS_LOGRADOUROS",
            "CIDADE_RH",
            "DISTRITO_RH",
            "LOGRADOURO_RH",
            List.of()
    ),

    SANE(
            OrigemDistrito.SANE,
            "SANE_LOGRADOUROS",
            "SANE_TIPOS_LOGRADOUROS",
            "CIDADE_SANE",
            "DISTRITO_SANE",
            "LOGRADOURO_SANE",
            List.of()
    ),

    SAU(
            OrigemDistrito.SAU,
            "SAU_LOGRADOUROS",
            "SAU_TIPOS_LOGRADOUROS",
            "CIDADE_SAUDE",
            "DISTRITO_SAUDE",
            "LOGRADOURO_SAUDE",
            List.of(
                    Complemento.numero("CEP",
                            "case when exists (select 1 from DBO_CORREIOS.CEPS_CARGA_DNE c"
                                    + " where c.cep = o.cep) then o.cep end"),
                    Complemento.numero("NUMERO_INI", "o.numero_ini"),
                    Complemento.numero("NUMERO_FIM", "o.numero_fim"),
                    Complemento.outro("LADO", "substr(o.lado, 1, 1)"),
                    Complemento.numero("LOGRADOURO_DNE", "o.logradouro_dne"),
                    Complemento.numero("BAIRRO_DNE", "o.bairro_dne"),
                    Complemento.numero("BAIRRO_DNE_FINAL", "o.bairro_dne_final"),
                    Complemento.outro("COMPLEMENTO", "substr(o.complemento, 1, 100)")
            )
    ),

    ESCO(
            OrigemDistrito.ESCO,
            "ESCO_LOGRADOUROS",
            "ESCO_TIPOS_LOGRADOUROS",
            "CIDADE_ESCOLA",
            "DISTRITO_ESCOLA",
            "LOGRADOURO_ESCOLA",
            List.of()
    );

    /*
     * Coluna de LOGRADOUROS_UNIFICADO completada com uma expressão da origem
     * (alias "o"). Numérico: 0 conta como vazio.
     */
    public record Complemento(
            String coluna,
            String expressao,
            boolean numerico
    ) {
        static Complemento numero(String coluna, String expressao) {
            return new Complemento(coluna, expressao, true);
        }

        static Complemento outro(String coluna, String expressao) {
            return new Complemento(coluna, expressao, false);
        }
    }

    private final OrigemDistrito origemDistrito;
    private final String tabelaLogradouros;
    private final String tabelaTipos;
    private final String colunaCidade;
    private final String colunaDistrito;
    private final String colunaLogradouro;
    private final List<Complemento> complementos;

    OrigemLogradouro(
            OrigemDistrito origemDistrito,
            String tabelaLogradouros,
            String tabelaTipos,
            String colunaCidade,
            String colunaDistrito,
            String colunaLogradouro,
            List<Complemento> complementos) {

        this.origemDistrito = origemDistrito;
        this.tabelaLogradouros = tabelaLogradouros;
        this.tabelaTipos = tabelaTipos;
        this.colunaCidade = colunaCidade;
        this.colunaDistrito = colunaDistrito;
        this.colunaLogradouro = colunaLogradouro;
        this.complementos = complementos;
    }

    public static OrigemLogradouro from(String valor) {

        if (valor == null || valor.isBlank()) {
            throw new NegocioException("Origem não informada.");
        }

        try {
            return OrigemLogradouro.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Origem inválida: " + valor + ".");
        }
    }

    public OrigemDistrito getOrigemDistrito() {
        return origemDistrito;
    }

    public String getDescricao() {
        return origemDistrito.getDescricao();
    }

    public String getStatusCor() {
        return origemDistrito.getStatusCor();
    }

    public String getTabelaLogradourosCompleta() {
        return OrigemCidade.SCHEMA_UNIFICADO + "." + tabelaLogradouros;
    }

    public String getTabelaTiposCompleta() {
        return OrigemCidade.SCHEMA_UNIFICADO + "." + tabelaTipos;
    }

    public String getTabelaCidadesCompleta() {
        return origemDistrito.getTabelaCidadesCompleta();
    }

    public String getColunaCidade() {
        return colunaCidade;
    }

    public String getColunaDistrito() {
        return colunaDistrito;
    }

    public String getColunaLogradouro() {
        return colunaLogradouro;
    }

    public List<Complemento> getComplementos() {
        return complementos;
    }
}
