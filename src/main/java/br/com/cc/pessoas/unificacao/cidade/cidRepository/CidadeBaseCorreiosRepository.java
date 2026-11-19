package br.com.cc.pessoas.unificacao.cidade.cidRepository;

import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/*
 * Base dos Correios em DBO_UNI_PESSOAS.CIDADES_UNIFICADO.
 *
 * Substitui o primeiro passo da SP_CIDADES_GERA_UNIFICADO
 * ("BASE PRINCIPAL: CORREIOS"): uma linha em CIDADES_UNIFICADO
 * para cada cidade de DBO_CORREIOS.CIDADES, com os códigos
 * de todos os sistemas zerados.
 *
 * Os códigos de cada sistema são gravados depois, pela tela,
 * origem por origem.
 */
@Repository
public class CidadeBaseCorreiosRepository {

    private static final String SCHEMA =
            OrigemCidade.SCHEMA_UNIFICADO;

    private static final String TABELA_UNIFICADO =
            SCHEMA + ".CIDADES_UNIFICADO";

    private static final String TABELA_CTRL =
            SCHEMA + ".CIDADES_UNIFICADO_CTRL";

    private static final String TABELA_CORREIOS =
            "DBO_CORREIOS.CIDADES";

    @PersistenceContext
    private EntityManager manager;

    public Long contarCorreios() {
        return count("select count(*) from " + TABELA_CORREIOS);
    }

    public Long contarCorreiosFicticias() {
        return count(
                "select count(*) from " + TABELA_CORREIOS
                        + " where status = 'N'"
        );
    }

    public Long contarUnificado() {
        return count("select count(*) from " + TABELA_UNIFICADO);
    }

    public Long contarFaltandoNaBase() {
        return count(
                "select count(*) from " + TABELA_CORREIOS + " c"
                        + " where not exists (select 1 from " + TABELA_UNIFICADO
                        + " cu where cu.cidade_correios = c.cidade)"
        );
    }

    /*
     * Insere em CIDADES_UNIFICADO as cidades dos Correios
     * que ainda não estão lá. Não altera nem apaga nada.
     *
     * STATUS_COR: 'G' para cidade dos Correios
     *             'N' para cidade criada fora dos Correios (STATUS = 'N').
     *
     * O DDD fica vazio e é completado quando a cidade
     * for vinculada a uma origem que tenha DDD.
     */
    public long inserirFaltantes() {

        StringBuilder colunasOrigem = new StringBuilder();
        StringBuilder zeros = new StringBuilder();

        for (OrigemCidade origem : OrigemCidade.values()) {
            colunasOrigem.append(origem.getColunaUnificado()).append(", ");
            zeros.append("0, ");
        }

        String sql = "insert into " + TABELA_UNIFICADO
                + " (ID, CIDADE_CORREIOS, "
                + colunasOrigem
                + "NOME, ESTADO, CEP, CIDADE_SICOM, CIDADE_INEP, CODIGO_IBGE, DDD,"
                + " DATA_ALTERACAO, USUARIO_ALTERACAO, STATUS_COR)"
                + " select " + SCHEMA + ".SEQ_CIDADES_UNIFICADO.nextval,"
                + " c.cidade, "
                + zeros
                + " c.nome,"
                + " upper(trim(c.estado)),"
                + " c.cep,"
                + " 0,"
                + " 0,"
                + " nvl(c.cd_ibge, 0),"
                + " null,"
                + " sysdate,"
                + " null,"
                + " case when c.status = 'N' then 'N' else 'G' end"
                + " from " + TABELA_CORREIOS + " c"
                + " where not exists (select 1 from " + TABELA_UNIFICADO
                + " cu where cu.cidade_correios = c.cidade)";

        return manager.createNativeQuery(sql).executeUpdate();
    }

    public long apagarControle() {
        return manager.createNativeQuery(
                "delete from " + TABELA_CTRL
        ).executeUpdate();
    }

    public long apagarUnificado() {
        return manager.createNativeQuery(
                "delete from " + TABELA_UNIFICADO
        ).executeUpdate();
    }

    /*
     * Apaga as cidades criadas fora dos Correios (STATUS = 'N')
     * que NÃO estão sendo usadas em nenhum lugar conhecido:
     *
     * - distritos, bairros e logradouros dos Correios;
     * - DBO_UNI_PESSOAS.DISTRITOS (usada na migração de pessoas);
     * - de-paras de distritos, bairros e logradouros unificados.
     *
     * As que estiverem em uso continuam e voltam para a base
     * com o mesmo código, preservando o que já depende delas.
     */
    public long apagarFicticiasSemUso() {

        String sql = "delete from " + TABELA_CORREIOS + " c"
                + " where c.status = 'N'"
                + " and not exists (select 1 from DBO_CORREIOS.DISTRITOS d where d.cidade = c.cidade)"
                + " and not exists (select 1 from DBO_CORREIOS.BAIRROS b where b.cidade = c.cidade)"
                + " and not exists (select 1 from DBO_CORREIOS.LOGRADOUROS l where l.cidade = c.cidade)"
                + " and not exists (select 1 from " + SCHEMA
                + ".DISTRITOS dd where dd.cidade = c.cidade)"
                + " and not exists (select 1 from " + SCHEMA
                + ".DISTRITOS_UNIFICADO du where du.cidade_correios = c.cidade)"
                + " and not exists (select 1 from " + SCHEMA
                + ".BAIRROS_UNIFICADO bu where bu.cidade_correios = c.cidade)"
                + " and not exists (select 1 from " + SCHEMA
                + ".LOGRADOUROS_UNIFICADO lu where lu.cidade_correios = c.cidade)";

        return manager.createNativeQuery(sql).executeUpdate();
    }

    private Long count(String sql) {
        Object result = manager.createNativeQuery(sql).getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }
}
