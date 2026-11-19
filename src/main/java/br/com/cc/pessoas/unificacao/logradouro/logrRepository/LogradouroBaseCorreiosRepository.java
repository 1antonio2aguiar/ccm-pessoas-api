package br.com.cc.pessoas.unificacao.logradouro.logrRepository;

import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import br.com.cc.pessoas.unificacao.logradouro.logrEnum.OrigemLogradouro;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Set;

/*
 * Base dos Correios em DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO.
 *
 * Uma linha em LOGRADOUROS_UNIFICADO para cada logradouro de
 * DBO_CORREIOS.LOGRADOUROS, com os códigos de todos os sistemas zerados
 * e as chaves de comparação (CHAVE_NOME / CHAVE_TIPO) calculadas.
 *
 * Diferente da SP_LOGRADOUROS_GERA_UNIFICADO, NÃO apaga os logradouros
 * STATUS = 'N' dos Correios e não altera DBO_CORREIOS.LOGRADOUROS.
 *
 * São cerca de 1,3 milhão de linhas: gerar do zero leva alguns minutos.
 */
@Repository
public class LogradouroBaseCorreiosRepository {

    private static final String SCHEMA =
            OrigemCidade.SCHEMA_UNIFICADO;

    private static final String TABELA_UNIFICADO =
            SCHEMA + ".LOGRADOUROS_UNIFICADO";

    private static final String TABELA_CTRL =
            SCHEMA + ".LOGRADOUROS_UNIFICADO_CTRL";

    private static final String TABELA_CORREIOS =
            "DBO_CORREIOS.LOGRADOUROS";

    private static final String PKG_CHAVE =
            SCHEMA + ".PKG_LOGRADOURO_CHAVE";

    private static final String FILTRO_CORREIOS =
            " where nvl(l.cidade, 0) > 0 and nvl(l.logradouro, 0) > 0";

    private static final String NAO_EXISTE_NA_BASE =
            " and not exists (select 1 from " + TABELA_UNIFICADO
                    + " lu where lu.cidade_correios = l.cidade"
                    + " and lu.distrito_correios = l.distrito"
                    + " and lu.logradouro_correios = l.logradouro)";

    private static final String FILTRO_SEM_CHAVE =
            " where nvl(lu.cidade_correios, 0) > 0"
                    + " and lu.nome is not null"
                    + " and lu.chave_nome is null";

    @PersistenceContext
    private EntityManager manager;

    public Long contarCorreios() {
        return count("select count(*) from " + TABELA_CORREIOS + " l" + FILTRO_CORREIOS);
    }

    public Long contarCorreiosFicticios() {
        return count(
                "select count(*) from " + TABELA_CORREIOS + " l"
                        + FILTRO_CORREIOS + " and l.status = 'N'"
        );
    }

    public Long contarUnificado() {
        return count("select count(*) from " + TABELA_UNIFICADO);
    }

    public Long contarFaltandoNaBase() {
        return count(
                "select count(*) from " + TABELA_CORREIOS + " l"
                        + FILTRO_CORREIOS + NAO_EXISTE_NA_BASE
        );
    }

    public Long contarSemChave() {
        return count("select count(*) from " + TABELA_UNIFICADO + " lu" + FILTRO_SEM_CHAVE);
    }

    /*
     * Insere os logradouros dos Correios que ainda não estão em LOGRADOUROS_UNIFICADO.
     *
     * NOME           = nome dos Correios (sem tipo e sem título);
     * NOME_CORREIOS  = nome completo dos Correios;
     * STATUS_COR     = 'G' logradouro dos Correios; 'N' criado fora dos Correios.
     */
    public long inserirFaltantes() {

        StringBuilder colunasOrigem = new StringBuilder();
        StringBuilder zeros = new StringBuilder();

        for (OrigemLogradouro origem : OrigemLogradouro.values()) {
            colunasOrigem.append(origem.getColunaCidade()).append(", ")
                    .append(origem.getColunaDistrito()).append(", ")
                    .append(origem.getColunaLogradouro()).append(", ");
            zeros.append("0, 0, 0, ");
        }

        String sql = "insert into " + TABELA_UNIFICADO
                + " (ID, CIDADE_CORREIOS, DISTRITO_CORREIOS, LOGRADOURO_CORREIOS, "
                + colunasOrigem
                + "BAIRRO, TIPO_LOGRADOURO, PREPOSICAO, TITULO_PATENTE,"
                + " NOME, NOME_CORREIOS, NOME_REDUZIDO, COMPLEMENTO,"
                + " CHAVE_NOME, CHAVE_TIPO,"
                + " DATA_ALTERACAO, USUARIO_ALTERACAO, STATUS_COR)"
                + " select " + SCHEMA + ".SEQ_LOGRADOUROS_UNIFICADO.nextval,"
                + " l.cidade,"
                + " l.distrito,"
                + " l.logradouro, "
                + zeros
                + " l.bairro,"
                + " l.tipo_logradouro,"
                + " l.preposicao,"
                + " l.titulo_patente,"
                + " l.nome,"
                + " substr(nvl(l.nome_completo,"
                + "   trim(l.titulo_patente || ' ' || l.preposicao || ' ' || l.nome)), 1, 80),"
                + " substr(l.nome_reduzido, 1, 90),"
                + " l.complemento,"
                + " " + PKG_CHAVE + ".chave(l.titulo_patente, l.preposicao, l.nome),"
                + " " + PKG_CHAVE + ".tipo(l.tipo_logradouro),"
                + " sysdate,"
                + " null,"
                + " case when l.status = 'N' then 'N' else 'G' end"
                + " from " + TABELA_CORREIOS + " l"
                + FILTRO_CORREIOS
                + NAO_EXISTE_NA_BASE;

        return manager.createNativeQuery(sql).executeUpdate();
    }

    /*
     * Calcula as chaves que estão vazias (linhas da carga antiga ou
     * chaves zeradas depois de alterar ABREVIATURAS_LOGRADOURO).
     */
    public long calcularChavesVazias() {

        String sql = "update " + TABELA_UNIFICADO + " lu"
                + " set lu.chave_nome = " + PKG_CHAVE
                + ".chave(lu.titulo_patente, lu.preposicao, lu.nome),"
                + " lu.chave_tipo = " + PKG_CHAVE + ".tipo(lu.tipo_logradouro)"
                + FILTRO_SEM_CHAVE;

        return manager.createNativeQuery(sql).executeUpdate();
    }

    /*
     * ZERAR RÁPIDO
     *
     * A versão anterior apagava as 1,3 milhão de linhas e inseria tudo de novo,
     * recalculando as chaves (mais de 20 minutos). Os logradouros dos Correios
     * não mudam ao zerar: o que muda são os vínculos. Agora:
     *
     * 1. apaga as linhas sem código dos Correios (carga antiga da procedure);
     * 2. apaga linhas repetidas para o mesmo logradouro dos Correios (fica a de menor ID);
     * 3. zera os códigos das origens e limpa os complementos, SÓ nas linhas que
     *    têm algum vínculo ou complemento (o custo é proporcional ao que foi
     *    vinculado, não ao tamanho da base);
     * 4. depois o service insere os logradouros que faltarem e calcula as chaves vazias.
     */
    public long apagarSemCodigoCorreios() {
        return manager.createNativeQuery(
                "delete from " + TABELA_UNIFICADO
                        + " where nvl(cidade_correios, 0) = 0"
                        + "    or nvl(logradouro_correios, 0) = 0"
        ).executeUpdate();
    }

    public long apagarRepetidos() {
        return manager.createNativeQuery(
                "delete from " + TABELA_UNIFICADO
                        + " where rowid in ("
                        + "   select rid from ("
                        + "     select rowid as rid,"
                        + "            row_number() over (partition by cidade_correios,"
                        + "              distrito_correios, logradouro_correios order by id) as rn"
                        + "       from " + TABELA_UNIFICADO
                        + "   ) where rn > 1"
                        + " )"
        ).executeUpdate();
    }

    public long zerarVinculos() {

        StringBuilder sets = new StringBuilder();
        StringBuilder condicao = new StringBuilder();

        for (OrigemLogradouro origem : OrigemLogradouro.values()) {
            for (String coluna : new String[]{
                    origem.getColunaCidade(), origem.getColunaDistrito(), origem.getColunaLogradouro()}) {

                sets.append("u.").append(coluna).append(" = 0, ");
                condicao.append(condicao.length() > 0 ? " or " : "")
                        .append("nvl(u.").append(coluna).append(", 0) <> 0");
            }
        }

        Set<String> complementos = new LinkedHashSet<>();

        for (OrigemLogradouro origem : OrigemLogradouro.values()) {
            for (OrigemLogradouro.Complemento complemento : origem.getComplementos()) {
                complementos.add(complemento.coluna());
            }
        }

        for (String coluna : complementos) {

            if ("COMPLEMENTO".equals(coluna)) {
                // complemento é dos Correios (faixa); a Saúde só preenchia quando vazio
                sets.append("u.COMPLEMENTO = case when nvl(u.")
                        .append(OrigemLogradouro.SAU.getColunaCidade()).append(", 0) <> 0")
                        .append(" then (select l.complemento from ").append(TABELA_CORREIOS)
                        .append(" l where l.cidade = u.cidade_correios")
                        .append(" and l.distrito = u.distrito_correios")
                        .append(" and l.logradouro = u.logradouro_correios)")
                        .append(" else u.COMPLEMENTO end, ");
                continue;
            }

            sets.append("u.").append(coluna).append(" = null, ");
            condicao.append(" or u.").append(coluna).append(" is not null");
        }

        String sql = "update " + TABELA_UNIFICADO + " u set "
                + sets
                + "u.data_alteracao = sysdate"
                + " where " + condicao;

        return manager.createNativeQuery(sql).executeUpdate();
    }

    public long apagarControle() {
        return manager.createNativeQuery("delete from " + TABELA_CTRL).executeUpdate();
    }

    public long apagarUnificado() {
        return manager.createNativeQuery("delete from " + TABELA_UNIFICADO).executeUpdate();
    }

    private Long count(String sql) {
        Object result = manager.createNativeQuery(sql).getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }
}
