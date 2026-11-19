package br.com.cc.pessoas.unificacao.cidade.cidEnum;

import br.com.cc.pessoas.service.exceptions.NegocioException;

/*
 * Configuração de cada sistema de origem na unificação de cidades.
 *
 * Cada origem informa:
 *
 * - o sinônimo da tabela de cidades no schema DBO_UNI_PESSOAS;
 * - a coluna de DBO_UNI_PESSOAS.CIDADES_UNIFICADO que guarda o código;
 * - o STATUS_COR gravado quando a cidade nasce a partir desta origem;
 * - as expressões SQL das colunas opcionais (CEP, DDD, IBGE, SICOM e INEP).
 *
 * Quando a origem não possui a coluna, a expressão é "null".
 *
 * Para incluir uma nova origem, basta acrescentar uma constante aqui.
 * Os valores são fixos no código e nunca vêm da requisição,
 * por isso podem ser concatenados no SQL com segurança.
 */
public enum OrigemCidade {

    PES(
            "PESSOAS",
            "PES_CIDADES",
            "CIDADE_PESSOA",
            "P",
            "o.cep",
            "o.ddd",
            "null",
            "o.cidade_sicom",
            "null"
    ),

    RH(
            "RH",
            "RH_CIDADES",
            "CIDADE_RH",
            "R",
            "o.cep",
            "o.ddd",
            "o.codigo_ibge",
            "null",
            "null"
    ),

    SANE(
            "SANEAMENTO",
            "SANE_CIDADES",
            "CIDADE_SANE",
            "S",
            "o.cep",
            "o.ddd",
            "null",
            "null",
            "null"
    ),

    SAU(
            "SAÚDE",
            "SAU_CIDADES",
            "CIDADE_SAUDE",
            "A",
            "null",
            "o.ddd",
            "null",
            "null",
            "null"
    ),

    ESCO(
            "ESCOLA",
            "ESCO_CIDADES",
            "CIDADE_ESCOLA",
            "E",
            "o.cep",
            "o.ddd",
            "null",
            "o.cidade_sicom",
            "o.cidade_inep"
    );

    /*
     * Schema onde estão os sinônimos (PES_, RH_, SANE_, SAU_, ESCO_),
     * a tabela CIDADES_UNIFICADO e a função FN_NORMALIZA_CIDADE.
     */
    public static final String SCHEMA_UNIFICADO = "DBO_UNI_PESSOAS";

    private final String descricao;
    private final String tabelaOrigem;
    private final String colunaUnificado;
    private final String statusCor;
    private final String expressaoCep;
    private final String expressaoDdd;
    private final String expressaoIbge;
    private final String expressaoSicom;
    private final String expressaoInep;

    OrigemCidade(
            String descricao,
            String tabelaOrigem,
            String colunaUnificado,
            String statusCor,
            String expressaoCep,
            String expressaoDdd,
            String expressaoIbge,
            String expressaoSicom,
            String expressaoInep) {

        this.descricao = descricao;
        this.tabelaOrigem = tabelaOrigem;
        this.colunaUnificado = colunaUnificado;
        this.statusCor = statusCor;
        this.expressaoCep = expressaoCep;
        this.expressaoDdd = expressaoDdd;
        this.expressaoIbge = expressaoIbge;
        this.expressaoSicom = expressaoSicom;
        this.expressaoInep = expressaoInep;
    }

    /*
     * Converte o valor recebido na URL (pes, rh, sane...).
     */
    public static OrigemCidade from(String valor) {

        if (valor == null || valor.isBlank()) {
            throw new NegocioException(
                    "Origem não informada."
            );
        }

        try {
            return OrigemCidade.valueOf(
                    valor.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw new NegocioException(
                    "Origem inválida: " + valor + "."
            );
        }
    }

    public String getDescricao() {
        return descricao;
    }

    public String getTabelaOrigemCompleta() {
        return SCHEMA_UNIFICADO + "." + tabelaOrigem;
    }

    public String getColunaUnificado() {
        return colunaUnificado;
    }

    public String getStatusCor() {
        return statusCor;
    }

    /*
     * As colunas numéricas passam por regexp_replace para aceitar
     * origens que guardam CEP/códigos como texto com máscara.
     */
    public String getExpressaoCep() {
        return numerico(expressaoCep);
    }

    public String getExpressaoIbge() {
        return numerico(expressaoIbge);
    }

    public String getExpressaoSicom() {
        return numerico(expressaoSicom);
    }

    public String getExpressaoInep() {
        return numerico(expressaoInep);
    }

    public String getExpressaoDdd() {

        if ("null".equals(expressaoDdd)) {
            return "cast(null as varchar2(4))";
        }

        return "trim(to_char(" + expressaoDdd + "))";
    }

    private static String numerico(String expressao) {

        if ("null".equals(expressao)) {
            return "cast(null as number)";
        }

        return "to_number(regexp_replace(to_char("
                + expressao
                + "), '[^0-9]', ''))";
    }
}
