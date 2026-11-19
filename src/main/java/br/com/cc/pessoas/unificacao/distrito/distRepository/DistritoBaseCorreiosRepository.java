package br.com.cc.pessoas.unificacao.distrito.distRepository;

import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import br.com.cc.pessoas.unificacao.distrito.distEnum.OrigemDistrito;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/*
 * Base dos Correios em DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO.
 *
 * Substitui o primeiro passo da SP_DISTRITOS_GERA_UNIFICADO
 * ("BASE PRINCIPAL: CORREIOS"): uma linha em DISTRITOS_UNIFICADO
 * para cada distrito de DBO_CORREIOS.DISTRITOS (com cidade),
 * com os códigos de todos os sistemas zerados.
 *
 * Diferente da procedure, NÃO altera o STATUS de DBO_CORREIOS.DISTRITOS.
 */
@Repository
public class DistritoBaseCorreiosRepository {

    private static final String SCHEMA =
            OrigemCidade.SCHEMA_UNIFICADO;

    private static final String TABELA_UNIFICADO =
            SCHEMA + ".DISTRITOS_UNIFICADO";

    private static final String TABELA_CTRL =
            SCHEMA + ".DISTRITOS_UNIFICADO_CTRL";

    private static final String TABELA_CORREIOS =
            "DBO_CORREIOS.DISTRITOS";

    private static final String FILTRO_CORREIOS =
            " where nvl(d.cidade, 0) > 0 and nvl(d.distrito, 0) > 0";

    @PersistenceContext
    private EntityManager manager;

    public Long contarCorreios() {
        return count("select count(*) from " + TABELA_CORREIOS + " d" + FILTRO_CORREIOS);
    }

    public Long contarCorreiosFicticios() {
        return count(
                "select count(*) from " + TABELA_CORREIOS + " d"
                        + FILTRO_CORREIOS + " and d.status = 'N'"
        );
    }

    public Long contarUnificado() {
        return count("select count(*) from " + TABELA_UNIFICADO);
    }

    public Long contarFaltandoNaBase() {
        return count(
                "select count(*) from " + TABELA_CORREIOS + " d"
                        + FILTRO_CORREIOS
                        + " and not exists (select 1 from " + TABELA_UNIFICADO
                        + " du where du.cidade_correios = d.cidade"
                        + " and du.distrito_correios = d.distrito)"
        );
    }

    /*
     * Insere os distritos dos Correios que ainda não estão
     * em DISTRITOS_UNIFICADO. Não altera nem apaga nada.
     *
     * STATUS_COR: 'G' para distrito dos Correios,
     *             'N' para distrito criado fora dos Correios (STATUS = 'N').
     */
    public long inserirFaltantes() {

        StringBuilder colunasOrigem = new StringBuilder();
        StringBuilder zeros = new StringBuilder();

        for (OrigemDistrito origem : OrigemDistrito.values()) {
            colunasOrigem.append(origem.getColunaCidade()).append(", ")
                    .append(origem.getColunaDistrito()).append(", ");
            zeros.append("0, 0, ");
        }

        String sql = "insert into " + TABELA_UNIFICADO
                + " (ID, CIDADE_CORREIOS, DISTRITO_CORREIOS, "
                + colunasOrigem
                + "NOME, ESTADO, DISTANCIA, CEP, CIDADE_INEP, DISTRITO_INEP,"
                + " DATA_ALTERACAO, USUARIO_ALTERACAO, STATUS_COR)"
                + " select " + SCHEMA + ".SEQ_DISTRITOS_UNIFICADO.nextval,"
                + " d.cidade,"
                + " d.distrito, "
                + zeros
                + " d.nome,"
                + " upper(trim(d.estado)),"
                + " null,"
                + " null,"
                + " 0,"
                + " 0,"
                + " sysdate,"
                + " null,"
                + " case when d.status = 'N' then 'N' else 'G' end"
                + " from " + TABELA_CORREIOS + " d"
                + FILTRO_CORREIOS
                + " and not exists (select 1 from " + TABELA_UNIFICADO
                + " du where du.cidade_correios = d.cidade"
                + " and du.distrito_correios = d.distrito)";

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
