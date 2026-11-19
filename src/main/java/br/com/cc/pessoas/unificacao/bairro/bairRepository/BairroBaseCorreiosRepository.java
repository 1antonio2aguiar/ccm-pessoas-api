package br.com.cc.pessoas.unificacao.bairro.bairRepository;

import br.com.cc.pessoas.unificacao.bairro.bairEnum.OrigemBairro;
import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/*
 * Base dos Correios em DBO_UNI_PESSOAS.BAIRROS_UNIFICADO.
 *
 * Uma linha em BAIRROS_UNIFICADO para cada bairro de DBO_CORREIOS.BAIRROS,
 * com os códigos de todos os sistemas zerados.
 *
 * Diferente da SP_BAIRROS_GERA_UNIFICADO, NÃO apaga os bairros
 * STATUS = 'N' dos Correios e não altera DBO_CORREIOS.BAIRROS.
 */
@Repository
public class BairroBaseCorreiosRepository {

    private static final String SCHEMA =
            OrigemCidade.SCHEMA_UNIFICADO;

    private static final String TABELA_UNIFICADO =
            SCHEMA + ".BAIRROS_UNIFICADO";

    private static final String TABELA_CTRL =
            SCHEMA + ".BAIRROS_UNIFICADO_CTRL";

    private static final String TABELA_CORREIOS =
            "DBO_CORREIOS.BAIRROS";

    private static final String FILTRO_CORREIOS =
            " where nvl(b.cidade, 0) > 0 and nvl(b.bairro, 0) > 0";

    private static final String NAO_EXISTE_NA_BASE =
            " and not exists (select 1 from " + TABELA_UNIFICADO
                    + " bu where bu.cidade_correios = b.cidade"
                    + " and bu.distrito_correios = b.distrito"
                    + " and bu.bairro_correios = b.bairro)";

    @PersistenceContext
    private EntityManager manager;

    public Long contarCorreios() {
        return count("select count(*) from " + TABELA_CORREIOS + " b" + FILTRO_CORREIOS);
    }

    public Long contarCorreiosFicticios() {
        return count(
                "select count(*) from " + TABELA_CORREIOS + " b"
                        + FILTRO_CORREIOS + " and b.status = 'N'"
        );
    }

    public Long contarUnificado() {
        return count("select count(*) from " + TABELA_UNIFICADO);
    }

    public Long contarFaltandoNaBase() {
        return count(
                "select count(*) from " + TABELA_CORREIOS + " b"
                        + FILTRO_CORREIOS + NAO_EXISTE_NA_BASE
        );
    }

    /*
     * Insere os bairros dos Correios que ainda não estão em BAIRROS_UNIFICADO.
     *
     * NOME e NOME_CORREIOS recebem o nome dos Correios.
     * STATUS_COR: 'G' bairro dos Correios; 'N' bairro criado fora dos Correios.
     */
    public long inserirFaltantes() {

        StringBuilder colunasOrigem = new StringBuilder();
        StringBuilder zeros = new StringBuilder();

        for (OrigemBairro origem : OrigemBairro.values()) {
            colunasOrigem.append(origem.getColunaCidade()).append(", ")
                    .append(origem.getColunaDistrito()).append(", ")
                    .append(origem.getColunaBairro()).append(", ");
            zeros.append("0, 0, 0, ");
        }

        String sql = "insert into " + TABELA_UNIFICADO
                + " (ID, CIDADE_CORREIOS, DISTRITO_CORREIOS, BAIRRO_CORREIOS, "
                + colunasOrigem
                + "NOME, NOME_CORREIOS, NOME_REDUZIDO, NOME_NORM,"
                + " DATA_ALTERACAO, USUARIO_ALTERACAO, STATUS_COR)"
                + " select " + SCHEMA + ".SEQ_BAIRROS_UNIFICADO.nextval,"
                + " b.cidade,"
                + " b.distrito,"
                + " b.bairro, "
                + zeros
                + " b.nome,"
                + " b.nome,"
                + " substr(b.nome_reduzido, 1, 40),"
                + " " + SCHEMA + ".FN_NORMALIZA_CIDADE(b.nome),"
                + " sysdate,"
                + " null,"
                + " case when b.status = 'N' then 'N' else 'G' end"
                + " from " + TABELA_CORREIOS + " b"
                + FILTRO_CORREIOS
                + NAO_EXISTE_NA_BASE;

        return manager.createNativeQuery(sql).executeUpdate();
    }

    /*
     * Nome normalizado gravado (NOME_NORM) para as linhas que ainda não têm:
     * a comparação usa a coluna indexada em vez de chamar a função em
     * todos os bairros a cada consulta.
     */
    public long calcularNomeNormVazio() {
        return manager.createNativeQuery(
                "update " + TABELA_UNIFICADO
                        + " set nome_norm = " + SCHEMA + ".FN_NORMALIZA_CIDADE(nome)"
                        + " where nome_norm is null and nome is not null"
        ).executeUpdate();
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
