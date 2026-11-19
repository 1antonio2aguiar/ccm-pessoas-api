package br.com.cc.pessoas.unificacao.distrito.distEnum;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;

/*
 * Configuração de cada sistema de origem na unificação de distritos.
 *
 * Cada origem informa:
 *
 * - a origem de cidade correspondente (OrigemCidade), usada para
 *   achar a cidade dos Correios pelo de-para de CIDADES_UNIFICADO
 *   e para ler o ESTADO da cidade da origem;
 * - o sinônimo da tabela de distritos em DBO_UNI_PESSOAS;
 * - as colunas CIDADE_x / DISTRITO_x de DISTRITOS_UNIFICADO;
 * - as expressões das colunas opcionais (DISTANCIA e CEP).
 *
 * O nome de cada constante é o mesmo de OrigemCidade
 * (PES, RH, SANE, SAU, ESCO).
 */
public enum OrigemDistrito {

    PES(
            OrigemCidade.PES,
            "PES_DISTRITOS",
            "CIDADE_PESSOA",
            "DISTRITO_PESSOA",
            "o.distancia",
            "null"
    ),

    RH(
            OrigemCidade.RH,
            "RH_DISTRITOS",
            "CIDADE_RH",
            "DISTRITO_RH",
            "null",
            "null"
    ),

    SANE(
            OrigemCidade.SANE,
            "SANE_DISTRITOS",
            "CIDADE_SANE",
            "DISTRITO_SANE",
            "null",
            "null"
    ),

    SAU(
            OrigemCidade.SAU,
            "SAU_DISTRITOS",
            "CIDADE_SAUDE",
            "DISTRITO_SAUDE",
            "null",
            "o.cep"
    ),

    ESCO(
            OrigemCidade.ESCO,
            "ESCO_DISTRITOS",
            "CIDADE_ESCOLA",
            "DISTRITO_ESCOLA",
            "null",
            "null"
    );

    private final OrigemCidade origemCidade;
    private final String tabelaDistritos;
    private final String colunaCidade;
    private final String colunaDistrito;
    private final String expressaoDistancia;
    private final String expressaoCep;

    OrigemDistrito(
            OrigemCidade origemCidade,
            String tabelaDistritos,
            String colunaCidade,
            String colunaDistrito,
            String expressaoDistancia,
            String expressaoCep) {

        this.origemCidade = origemCidade;
        this.tabelaDistritos = tabelaDistritos;
        this.colunaCidade = colunaCidade;
        this.colunaDistrito = colunaDistrito;
        this.expressaoDistancia = expressaoDistancia;
        this.expressaoCep = expressaoCep;
    }

    public static OrigemDistrito from(String valor) {

        if (valor == null || valor.isBlank()) {
            throw new NegocioException(
                    "Origem não informada."
            );
        }

        try {
            return OrigemDistrito.valueOf(
                    valor.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw new NegocioException(
                    "Origem inválida: " + valor + "."
            );
        }
    }

    public OrigemCidade getOrigemCidade() {
        return origemCidade;
    }

    public String getDescricao() {
        return origemCidade.getDescricao();
    }

    public String getStatusCor() {
        return origemCidade.getStatusCor();
    }

    public String getTabelaDistritosCompleta() {
        return OrigemCidade.SCHEMA_UNIFICADO + "." + tabelaDistritos;
    }

    public String getTabelaCidadesCompleta() {
        return origemCidade.getTabelaOrigemCompleta();
    }

    /*
     * Coluna de CIDADES_UNIFICADO com o código da cidade desta origem.
     */
    public String getColunaCidadeUnificado() {
        return origemCidade.getColunaUnificado();
    }

    public String getColunaCidade() {
        return colunaCidade;
    }

    public String getColunaDistrito() {
        return colunaDistrito;
    }

    public String getExpressaoDistancia() {

        if ("null".equals(expressaoDistancia)) {
            return "cast(null as number)";
        }

        return "to_number(" + expressaoDistancia + ")";
    }

    public String getExpressaoCep() {

        if ("null".equals(expressaoCep)) {
            return "cast(null as number)";
        }

        return "to_number(regexp_replace(to_char("
                + expressaoCep
                + "), '[^0-9]', ''))";
    }
}
