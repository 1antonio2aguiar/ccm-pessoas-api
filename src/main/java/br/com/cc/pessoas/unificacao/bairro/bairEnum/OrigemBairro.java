package br.com.cc.pessoas.unificacao.bairro.bairEnum;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import br.com.cc.pessoas.unificacao.distrito.distEnum.OrigemDistrito;

import java.util.List;

/*
 * Configuração de cada sistema de origem na unificação de bairros.
 *
 * Cada origem informa:
 *
 * - a origem de distrito correspondente (OrigemDistrito), usada para
 *   achar cidade + distrito dos Correios pelo de-para de DISTRITOS_UNIFICADO;
 * - o sinônimo da tabela de bairros em DBO_UNI_PESSOAS;
 * - as colunas CIDADE_x / DISTRITO_x / BAIRRO_x de BAIRROS_UNIFICADO;
 * - os COMPLEMENTOS: colunas de BAIRROS_UNIFICADO preenchidas com dados
 *   da origem quando estiverem vazias (nunca sobrescreve).
 *
 * Os complementos de macro bairro vêm só do PESSOAS: os códigos de macro
 * bairro de cada sistema são diferentes e não podem ser misturados.
 */
public enum OrigemBairro {

    PES(
            OrigemDistrito.PES,
            "PES_BAIRROS",
            "CIDADE_PESSOA",
            "DISTRITO_PESSOA",
            "BAIRRO_PESSOA",
            List.of(
                    Complemento.numero("CIDADE_MACRO_BAIRRO", "o.cidade_macro_bairro"),
                    Complemento.numero("DISTRITO_MACRO_BAIRRO", "o.distrito_macro_bairro"),
                    Complemento.numero("MACRO_BAIRRO", "o.macro_bairro"),
                    Complemento.outro("DATA_CRIACAO", "o.data_criacao"),
                    Complemento.numero("LEI_CRIACAO", "o.lei_criacao"),
                    Complemento.outro("DATA_DECRETO", "o.data_decreto"),
                    Complemento.numero("DECRETO", "o.decreto"),
                    Complemento.outro("DATA_PORTARIA", "o.data_portaria"),
                    Complemento.numero("PORTARIA", "o.portaria"),
                    Complemento.outro("NOME_VEREADOR", "o.nome_vereador"),
                    Complemento.numero("PESSOA_LOTEADORA", "o.pessoa_loteadora"),
                    Complemento.outro("TIPO_BAIRRO", "o.tipo_bairro")
            )
    ),

    RH(
            OrigemDistrito.RH,
            "RH_BAIRROS",
            "CIDADE_RH",
            "DISTRITO_RH",
            "BAIRRO_RH",
            List.of(
                    Complemento.outro("ZONA_RURAL", "o.zona_rural")
            )
    ),

    SANE(
            OrigemDistrito.SANE,
            "SANE_BAIRROS",
            "CIDADE_SANE",
            "DISTRITO_SANE",
            "BAIRRO_SANE",
            List.of()
    ),

    SAU(
            OrigemDistrito.SAU,
            "SAU_BAIRROS",
            "CIDADE_SAUDE",
            "DISTRITO_SAUDE",
            "BAIRRO_SAUDE",
            List.of(
                    Complemento.numero("BAIRRO_DNE", "o.bairro_dne")
            )
    ),

    ESCO(
            OrigemDistrito.ESCO,
            "ESCO_BAIRROS",
            "CIDADE_ESCOLA",
            "DISTRITO_ESCOLA",
            "BAIRRO_ESCOLA",
            List.of()
    );

    /*
     * Coluna de BAIRROS_UNIFICADO completada com uma expressão da origem
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
    private final String tabelaBairros;
    private final String colunaCidade;
    private final String colunaDistrito;
    private final String colunaBairro;
    private final List<Complemento> complementos;

    OrigemBairro(
            OrigemDistrito origemDistrito,
            String tabelaBairros,
            String colunaCidade,
            String colunaDistrito,
            String colunaBairro,
            List<Complemento> complementos) {

        this.origemDistrito = origemDistrito;
        this.tabelaBairros = tabelaBairros;
        this.colunaCidade = colunaCidade;
        this.colunaDistrito = colunaDistrito;
        this.colunaBairro = colunaBairro;
        this.complementos = complementos;
    }

    public static OrigemBairro from(String valor) {

        if (valor == null || valor.isBlank()) {
            throw new NegocioException("Origem não informada.");
        }

        try {
            return OrigemBairro.valueOf(valor.trim().toUpperCase());
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

    public String getTabelaBairrosCompleta() {
        return OrigemCidade.SCHEMA_UNIFICADO + "." + tabelaBairros;
    }

    public String getTabelaCidadesCompleta() {
        return origemDistrito.getTabelaCidadesCompleta();
    }

    public String getTabelaDistritosCompleta() {
        return origemDistrito.getTabelaDistritosCompleta();
    }

    public String getColunaCidade() {
        return colunaCidade;
    }

    public String getColunaDistrito() {
        return colunaDistrito;
    }

    public String getColunaBairro() {
        return colunaBairro;
    }

    public List<Complemento> getComplementos() {
        return complementos;
    }
}
