package br.com.cc.pessoas.unificacao.consolidacao.consRepository;

import br.com.cc.pessoas.unificacao.consolidacao.consEnum.NivelConsolidacao;
import br.com.cc.pessoas.unificacao.consolidacao.consEnum.NivelConsolidacao.Fixo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.stream.Collectors;

/*
 * Consolidação incremental: um MERGE por nível.
 *
 * - linha nova (chave dos Correios ainda não consolidada): INSERT;
 * - linha existente: só as colunas vazias recebem valor (UPDATE ... WHERE coluna vazia);
 * - DELETE: nunca;
 * - CODIGO_CCM: só recebe o valor fixo na inclusão; nunca é alterado.
 */
@Repository
public class ConsolidacaoRepository {

    @PersistenceContext
    private EntityManager manager;

    public Long contarUnificado(NivelConsolidacao nivel) {
        return count("select count(*) from (" + nivel.getSqlOrigem() + ")");
    }

    public Long contarConsolidado(NivelConsolidacao nivel) {
        return count("select count(*) from " + nivel.getTabelaDestino());
    }

    public Long contarFaltando(NivelConsolidacao nivel) {
        return count(
                "select count(*) from (" + sqlChaves(nivel) + ") s"
                        + " where not exists (select 1 from " + nivel.getTabelaDestino()
                        + " d where " + condicaoChaves(nivel) + ")"
        );
    }

    public Long contarSoConsolidado(NivelConsolidacao nivel) {
        return count(
                "select count(*) from " + nivel.getTabelaDestino() + " d"
                        + " where not exists (select 1 from (" + sqlChaves(nivel) + ") s"
                        + " where " + condicaoChaves(nivel) + ")"
        );
    }

    /*
     * Executa o MERGE e devolve o total de linhas afetadas (inseridas + completadas).
     */
    public long mesclar(NivelConsolidacao nivel) {

        String completar =
                nivel.getColunasCompletar().stream()
                        .map(c -> "d." + c + " = nvl(d." + c + ", s." + c + ")")
                        .collect(Collectors.joining(", "));

        String condicaoCompletar =
                nivel.getColunasCompletar().stream()
                        .map(c -> "(d." + c + " is null and s." + c + " is not null)")
                        .collect(Collectors.joining(" or "));

        String colunasInsert =
                String.join(", ", nivel.getColunasInsert())
                        + nivel.getFixosInsert().stream()
                        .map(f -> ", " + f.coluna())
                        .collect(Collectors.joining());

        String valoresInsert =
                nivel.getColunasInsert().stream()
                        .map(c -> "s." + c)
                        .collect(Collectors.joining(", "))
                        + nivel.getFixosInsert().stream()
                        .map(Fixo::expressao)
                        .map(e -> ", " + e)
                        .collect(Collectors.joining());

        StringBuilder sql = new StringBuilder();

        sql.append("merge into ").append(nivel.getTabelaDestino()).append(" d")
                .append(" using (").append(nivel.getSqlOrigem()).append(") s")
                .append(" on (").append(condicaoChaves(nivel)).append(")");

        if (!nivel.getColunasCompletar().isEmpty()) {
            sql.append(" when matched then update set ").append(completar)
                    .append(" where ").append(condicaoCompletar);
        }

        sql.append(" when not matched then insert (").append(colunasInsert).append(")")
                .append(" values (").append(valoresInsert).append(")");

        return manager.createNativeQuery(sql.toString()).executeUpdate();
    }

    /*
     * Só as colunas da chave: as contagens não precisam calcular o resto
     * (no nível de logradouros, o tipo).
     */
    private String sqlChaves(NivelConsolidacao nivel) {
        return "select " + String.join(", ", nivel.getChaves())
                + " from (" + nivel.getSqlOrigem() + ")";
    }

    private String condicaoChaves(NivelConsolidacao nivel) {
        return nivel.getChaves().stream()
                .map(c -> "d." + c + " = s." + c)
                .collect(Collectors.joining(" and "));
    }

    private Long count(String sql) {
        Object result = manager.createNativeQuery(sql).getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }
}
