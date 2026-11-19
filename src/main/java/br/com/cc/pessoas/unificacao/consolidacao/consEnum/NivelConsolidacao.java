package br.com.cc.pessoas.unificacao.consolidacao.consEnum;

import br.com.cc.pessoas.service.exceptions.NegocioException;

import java.util.List;

/*
 * Níveis da consolidação (fase 2): das tabelas *_UNIFICADO para as tabelas
 * consolidadas de DBO_UNI_PESSOAS (CIDADES, DISTRITOS, BAIRROS, LOGRADOUROS, CEPS).
 *
 * Substitui SP_CIDADES_UNIFICA, SP_DISTRITOS_UNIFICA, SP_BAIRROS_UNIFICA,
 * SP_LOGRADOUROS_UNIFICA e SP_CEPS_UNIFICA, com outra regra:
 *
 * - NUNCA apaga a tabela consolidada (as procedures começavam com DELETE);
 * - insere o que falta, pela chave dos Correios;
 * - nas linhas que já existem, só preenche colunas VAZIAS (nunca sobrescreve);
 * - NUNCA toca em CODIGO_CCM (gerado na carga das tabelas finais, fase 3);
 * - não cria código nenhum: tudo já tem código dos Correios desde a unificação.
 *
 * Mapeamento de colunas igual ao das procedures. Cada nível informa:
 *
 * - sqlOrigem: select com as colunas do destino (mesmos nomes), uma linha por chave;
 * - chaves: colunas da chave da tabela consolidada;
 * - colunasInsert: colunas gravadas na inclusão;
 * - colunasCompletar: colunas preenchidas nas linhas existentes, se estiverem vazias;
 * - fixosInsert: colunas com valor fixo na inclusão (CODIGO_CCM, datas).
 */
public enum NivelConsolidacao {

    CIDADES(
            "Cidades",
            "DBO_UNI_PESSOAS.CIDADES",
            " select cidade, nome, estado, cep, ddd, cidade_sicom, cidade_inep, codigo_ibge"
                    + " from ("
                    + "   select cu.cidade_correios as cidade,"
                    + "          substr(nvl(cu.nome, cc.nome), 1, 40) as nome,"
                    + "          nvl(cu.estado, cc.estado) as estado,"
                    + "          nvl(cu.cep, cc.cep) as cep,"
                    + "          cu.ddd,"
                    + "          cu.cidade_sicom,"
                    + "          cu.cidade_inep,"
                    + "          nvl(cu.codigo_ibge, cc.cd_ibge) as codigo_ibge,"
                    + "          row_number() over (partition by cu.cidade_correios order by cu.id) as rn"
                    + "     from DBO_UNI_PESSOAS.CIDADES_UNIFICADO cu"
                    + "     left join DBO_CORREIOS.CIDADES cc"
                    + "       on cc.cidade = cu.cidade_correios"
                    + "    where nvl(cu.cidade_correios, 0) > 0"
                    + "      and nvl(cu.nome, cc.nome) is not null"
                    + "      and nvl(cu.estado, cc.estado) is not null"
                    + " ) where rn = 1",
            List.of("CIDADE"),
            List.of("CIDADE", "NOME", "ESTADO", "CEP", "DDD", "CIDADE_SICOM", "CIDADE_INEP", "CODIGO_IBGE"),
            List.of("CEP", "DDD", "CIDADE_SICOM", "CIDADE_INEP", "CODIGO_IBGE"),
            List.of(new Fixo("DATA_ALTERACAO", "sysdate"))
    ),

    DISTRITOS(
            "Distritos",
            "DBO_UNI_PESSOAS.DISTRITOS",
            " select cidade, distrito, nome, distancia, cep, cidade_inep, distrito_inep"
                    + " from ("
                    + "   select du.cidade_correios as cidade,"
                    + "          du.distrito_correios as distrito,"
                    + "          substr(nvl(du.nome, cd.nome), 1, 80) as nome,"
                    + "          du.distancia,"
                    + "          du.cep,"
                    + "          du.cidade_inep,"
                    + "          du.distrito_inep,"
                    + "          row_number() over (partition by du.cidade_correios, du.distrito_correios"
                    + "                             order by du.id) as rn"
                    + "     from DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO du"
                    + "     left join DBO_CORREIOS.DISTRITOS cd"
                    + "       on cd.cidade = du.cidade_correios"
                    + "      and cd.distrito = du.distrito_correios"
                    + "    where nvl(du.cidade_correios, 0) > 0"
                    + "      and du.distrito_correios is not null"
                    + " ) where rn = 1",
            List.of("CIDADE", "DISTRITO"),
            List.of("CIDADE", "DISTRITO", "NOME", "DISTANCIA", "CEP", "CIDADE_INEP", "DISTRITO_INEP"),
            List.of("NOME", "DISTANCIA", "CEP", "CIDADE_INEP", "DISTRITO_INEP"),
            List.of(new Fixo("CODIGO_CCM", "null"))
    ),

    BAIRROS(
            "Bairros",
            "DBO_UNI_PESSOAS.BAIRROS",
            " select cidade, distrito, bairro, nome, cidade_macro_bairro, distrito_macro_bairro,"
                    + " macro_bairro, data_criacao, lei_criacao, data_decreto, decreto, data_portaria,"
                    + " portaria, nome_vereador, pessoa_loteadora, tipo_bairro, bairro_dne,"
                    + " nome_abreviado, zona_rural"
                    + " from ("
                    + "   select bu.cidade_correios as cidade,"
                    + "          bu.distrito_correios as distrito,"
                    + "          bu.bairro_correios as bairro,"
                    + "          substr(bu.nome, 1, 80) as nome,"
                    + "          bu.cidade_macro_bairro, bu.distrito_macro_bairro, bu.macro_bairro,"
                    + "          bu.data_criacao, bu.lei_criacao, bu.data_decreto, bu.decreto,"
                    + "          bu.data_portaria, bu.portaria, bu.nome_vereador, bu.pessoa_loteadora,"
                    + "          bu.tipo_bairro, bu.bairro_dne,"
                    + "          substr(bu.nome_reduzido, 1, 40) as nome_abreviado,"
                    + "          bu.zona_rural,"
                    + "          row_number() over (partition by bu.cidade_correios, bu.distrito_correios,"
                    + "                             bu.bairro_correios order by bu.id) as rn"
                    + "     from DBO_UNI_PESSOAS.BAIRROS_UNIFICADO bu"
                    + "    where nvl(bu.cidade_correios, 0) > 0"
                    + "      and bu.distrito_correios is not null"
                    + "      and nvl(bu.bairro_correios, 0) > 0"
                    + " ) where rn = 1",
            List.of("CIDADE", "DISTRITO", "BAIRRO"),
            List.of("CIDADE", "DISTRITO", "BAIRRO", "NOME", "CIDADE_MACRO_BAIRRO", "DISTRITO_MACRO_BAIRRO",
                    "MACRO_BAIRRO", "DATA_CRIACAO", "LEI_CRIACAO", "DATA_DECRETO", "DECRETO",
                    "DATA_PORTARIA", "PORTARIA", "NOME_VEREADOR", "PESSOA_LOTEADORA", "TIPO_BAIRRO",
                    "BAIRRO_DNE", "NOME_ABREVIADO", "ZONA_RURAL"),
            List.of("NOME", "CIDADE_MACRO_BAIRRO", "DISTRITO_MACRO_BAIRRO", "MACRO_BAIRRO",
                    "DATA_CRIACAO", "LEI_CRIACAO", "DATA_DECRETO", "DECRETO", "DATA_PORTARIA",
                    "PORTARIA", "NOME_VEREADOR", "PESSOA_LOTEADORA", "TIPO_BAIRRO", "BAIRRO_DNE",
                    "NOME_ABREVIADO", "ZONA_RURAL"),
            List.of(new Fixo("DATA_ALTERACAO", "sysdate"), new Fixo("CODIGO_CCM", "0"))
    ),

    /*
     * TIPO_LOGRADOURO: código do tipo no PESSOAS (como na procedure), achado pela
     * descrição; 'NEC' quando não encontrado. TIPO_LOGRAD_DESC: descrição.
     * A subconsulta do tipo depende só de CHAVE_TIPO (poucos valores distintos),
     * e o Oracle guarda o resultado em cache durante a execução.
     */
    LOGRADOUROS(
            "Logradouros",
            "DBO_UNI_PESSOAS.LOGRADOUROS",
            " select cidade, distrito, logradouro, tipo_logradouro, tipo_lograd_desc, preposicao,"
                    + " titulo_patente, nome, nome1, nome_legal, nome_reduzido, cep, numero_ini,"
                    + " numero_fim, lado, logradouro_dne, bairro_dne, bairro_dne_final, complemento, bairro"
                    + " from ("
                    + "   select lu.cidade_correios as cidade,"
                    + "          lu.distrito_correios as distrito,"
                    + "          lu.logradouro_correios as logradouro,"
                    + "          substr(nvl((select min(t.tipo_logradouro)"
                    + "                        from DBO_UNI_PESSOAS.PES_TIPOS_LOGRADOUROS t"
                    + "                       where DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE.tipo(t.descricao)"
                    + "                             = lu.chave_tipo), 'NEC'), 1, 25) as tipo_logradouro,"
                    + "          substr(upper(lu.tipo_logradouro), 1, 30) as tipo_lograd_desc,"
                    + "          lu.preposicao,"
                    + "          lu.titulo_patente,"
                    + "          upper(lu.nome) as nome,"
                    + "          upper(lu.nome_correios) as nome1,"
                    + "          lu.nome_legal,"
                    + "          upper(lu.nome_reduzido) as nome_reduzido,"
                    + "          lu.cep, lu.numero_ini, lu.numero_fim, lu.lado,"
                    + "          lu.logradouro_dne, lu.bairro_dne, lu.bairro_dne_final,"
                    + "          lu.complemento, lu.bairro,"
                    + "          row_number() over (partition by lu.cidade_correios, lu.distrito_correios,"
                    + "                             lu.logradouro_correios order by lu.id) as rn"
                    + "     from DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO lu"
                    + "    where nvl(lu.cidade_correios, 0) > 0"
                    + "      and lu.distrito_correios is not null"
                    + "      and nvl(lu.logradouro_correios, 0) > 0"
                    + " ) where rn = 1",
            List.of("CIDADE", "DISTRITO", "LOGRADOURO"),
            List.of("CIDADE", "DISTRITO", "LOGRADOURO", "TIPO_LOGRADOURO", "TIPO_LOGRAD_DESC",
                    "PREPOSICAO", "TITULO_PATENTE", "NOME", "NOME1", "NOME_LEGAL", "NOME_REDUZIDO",
                    "CEP", "NUMERO_INI", "NUMERO_FIM", "LADO", "LOGRADOURO_DNE", "BAIRRO_DNE",
                    "BAIRRO_DNE_FINAL", "COMPLEMENTO", "BAIRRO"),
            List.of("NOME_LEGAL", "CEP", "NUMERO_INI", "NUMERO_FIM", "LADO", "LOGRADOURO_DNE",
                    "BAIRRO_DNE", "BAIRRO_DNE_FINAL", "COMPLEMENTO", "BAIRRO"),
            List.of(new Fixo("DATA_ALTERACAO", "sysdate"), new Fixo("CODIGO_CCM", "null"))
    ),

    /*
     * CEPS: CEPS_CARGA_DNE (como a SP_CEPS_UNIFICA) + os vínculos de CEP dos
     * logradouros criados pela tela (DBO_CORREIOS.CEPS com STATUS = 'N').
     * NUMERO_INI faz parte da chave e não aceita nulo: nulo vira 0.
     */
    CEPS(
            "CEPs",
            "DBO_UNI_PESSOAS.CEPS",
            " select cidade, distrito, logradouro, cep, numero_ini, numero_fim, identificacao, bairro"
                    + " from ("
                    + "   select x.*,"
                    + "          row_number() over (partition by x.cidade, x.distrito, x.logradouro,"
                    + "                             x.cep, x.numero_ini order by x.prioridade) as rn"
                    + "     from ("
                    + "       select c.cidade, c.distrito, c.logradouro, c.cep,"
                    + "              nvl(c.numero_ini, 0) as numero_ini,"
                    + "              c.numero_fin as numero_fim,"
                    + "              c.identificacao, c.bairro, 1 as prioridade"
                    + "         from DBO_CORREIOS.CEPS_CARGA_DNE c"
                    + "        where nvl(c.cidade, 0) > 0"
                    + "          and c.distrito is not null"
                    + "          and c.logradouro is not null"
                    + "          and c.cep is not null"
                    + "       union all"
                    + "       select c.cidade, c.distrito, c.logradouro, c.cep,"
                    + "              nvl(c.numero_ini, 0),"
                    + "              c.numero_fin,"
                    + "              c.identificacao, l.bairro, 2"
                    + "         from DBO_CORREIOS.CEPS c"
                    + "         left join DBO_CORREIOS.LOGRADOUROS l"
                    + "           on l.cidade = c.cidade"
                    + "          and l.distrito = c.distrito"
                    + "          and l.logradouro = c.logradouro"
                    + "        where c.status = 'N'"
                    + "     ) x"
                    + " ) where rn = 1",
            List.of("CIDADE", "DISTRITO", "LOGRADOURO", "CEP", "NUMERO_INI"),
            List.of("CIDADE", "DISTRITO", "LOGRADOURO", "CEP", "NUMERO_INI", "NUMERO_FIM",
                    "IDENTIFICACAO", "BAIRRO"),
            List.of("NUMERO_FIM", "IDENTIFICACAO", "BAIRRO"),
            List.of(new Fixo("CODIGO_CCM", "null"))
    );

    public record Fixo(String coluna, String expressao) {
    }

    private final String descricao;
    private final String tabelaDestino;
    private final String sqlOrigem;
    private final List<String> chaves;
    private final List<String> colunasInsert;
    private final List<String> colunasCompletar;
    private final List<Fixo> fixosInsert;

    NivelConsolidacao(
            String descricao,
            String tabelaDestino,
            String sqlOrigem,
            List<String> chaves,
            List<String> colunasInsert,
            List<String> colunasCompletar,
            List<Fixo> fixosInsert) {

        this.descricao = descricao;
        this.tabelaDestino = tabelaDestino;
        this.sqlOrigem = sqlOrigem;
        this.chaves = chaves;
        this.colunasInsert = colunasInsert;
        this.colunasCompletar = colunasCompletar;
        this.fixosInsert = fixosInsert;
    }

    public static NivelConsolidacao from(String valor) {

        if (valor == null || valor.isBlank()) {
            throw new NegocioException("Nível não informado.");
        }

        try {
            return NivelConsolidacao.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Nível inválido: " + valor + ".");
        }
    }

    public String getDescricao() {
        return descricao;
    }

    public String getTabelaDestino() {
        return tabelaDestino;
    }

    public String getSqlOrigem() {
        return sqlOrigem;
    }

    public List<String> getChaves() {
        return chaves;
    }

    public List<String> getColunasInsert() {
        return colunasInsert;
    }

    public List<String> getColunasCompletar() {
        return colunasCompletar;
    }

    public List<Fixo> getFixosInsert() {
        return fixosInsert;
    }
}
