package br.com.cc.pessoas.unificacao.distrito.distRepository;

import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoCandidatoDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoOrigemDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.distrito.distEnum.OrigemDistrito;
import br.com.cc.pessoas.unificacao.distrito.distFilter.DistritoUnificadoFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Consultas e gravações da unificação de distritos.
 *
 * Tabelas envolvidas:
 *
 * - DBO_UNI_PESSOAS.<ORIGEM>_DISTRITOS / <ORIGEM>_CIDADES (sinônimos, leitura)
 * - DBO_UNI_PESSOAS.CIDADES_UNIFICADO e CIDADES_UNIFICADO_CTRL (leitura:
 *   de-para da cidade da origem para a cidade dos Correios)
 * - DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO      (de-para por sistema)
 * - DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO_CTRL (duplicados e "não vincular")
 * - DBO_CORREIOS.DISTRITOS                   (somente inclusão de distrito novo, STATUS = 'N')
 *
 * Regra central: um distrito da origem só é comparado com os distritos
 * da MESMA cidade dos Correios, obtida pelo de-para de cidades.
 * Por isso a cidade precisa estar unificada antes do distrito.
 *
 * SQL compatível com Oracle 11g: sem WITH, sem FETCH FIRST, paginação por ROWNUM.
 */
@Repository
public class DistritoUnificadoRepository {

    private static final String SCHEMA =
            OrigemCidade.SCHEMA_UNIFICADO;

    private static final String TABELA_CIDADES_UNIFICADO =
            SCHEMA + ".CIDADES_UNIFICADO";

    private static final String TABELA_CIDADES_CTRL =
            SCHEMA + ".CIDADES_UNIFICADO_CTRL";

    private static final String TABELA_UNIFICADO =
            SCHEMA + ".DISTRITOS_UNIFICADO";

    private static final String TABELA_CTRL =
            SCHEMA + ".DISTRITOS_UNIFICADO_CTRL";

    private static final String FN_NORMALIZA =
            SCHEMA + ".FN_NORMALIZA_CIDADE";

    private static final String TABELA_CORREIOS =
            "DBO_CORREIOS.DISTRITOS";

    private static final String SEQ_CORREIOS =
            "DBO_CORREIOS.SEQ_DISTRITOS_FICTICIOS";

    private static final String SEQ_UNIFICADO =
            SCHEMA + ".SEQ_DISTRITOS_UNIFICADO";

    public static final String STATUS_DUPLICADO = "D";
    public static final String STATUS_NAO_VINCULAR = "N";

    @PersistenceContext
    private EntityManager manager;

    /*
     * Dados completos de um distrito da origem,
     * usados nas ações de vincular e criar.
     *
     * cidadeCorreios fica nulo quando a cidade da origem
     * não está unificada (ou está em mais de uma cidade).
     */
    public record DistritoOrigemDados(
            Long cidade,
            Long distrito,
            String nome,
            String uf,
            String nomeCidade,
            BigDecimal distancia,
            Long cep,
            String nomeNormalizado,
            Long cidadeCorreios,
            Long qtdCidadesCorreios
    ) {
    }

    /**********************************************************************
     * LISTAGEM
     **********************************************************************/

    public Page<DistritoOrigemDTO> filtrarPendentes(
            OrigemDistrito origem,
            DistritoUnificadoFilter filter,
            Pageable pageable) {

        StringBuilder sqlClassificado = new StringBuilder();

        sqlClassificado.append(" select c.*, count(*) over() as total_registros")
                .append(" from (")
                .append(sqlClassificacao(origem, filter))
                .append(" ) c")
                .append(" where 1 = 1");

        if (StringUtils.hasText(filter.getSituacao())) {
            sqlClassificado.append(" and c.situacao = :situacao");
        }

        sqlClassificado.append(" order by c.nome_cidade, c.nome, c.cidade_origem, c.distrito_origem");

        String sql = " select p.cidade_origem,"
                + " p.distrito_origem,"
                + " p.nome,"
                + " p.uf,"
                + " p.nome_cidade,"
                + " p.cidade_correios,"
                + " p.qtd_candidatos,"
                + " p.situacao,"
                + " p.id_sugerido,"
                + " s.nome as nome_sugerido,"
                + " s.distrito_correios as distrito_correios_sugerido,"
                + " p.total_registros"
                + " from ("
                + "   select b.*, rownum as rn"
                + "     from (" + sqlClassificado + ") b"
                + "    where rownum <= :fim"
                + " ) p"
                + " left join " + TABELA_UNIFICADO + " s"
                + "   on s.id = p.id_sugerido"
                + " where p.rn > :inicio"
                + " order by p.rn";

        Query query = manager.createNativeQuery(sql);

        aplicarParametros(origem, filter, query);

        if (StringUtils.hasText(filter.getSituacao())) {
            query.setParameter(
                    "situacao",
                    filter.getSituacao().trim().toUpperCase()
            );
        }

        long inicio = pageable.getOffset();

        query.setParameter("inicio", inicio);
        query.setParameter("fim", inicio + pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = query.getResultList();

        if (linhas == null || linhas.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0L);
        }

        Long total = toLong(linhas.get(0)[11]);

        List<DistritoOrigemDTO> lista =
                linhas.stream()
                        .map(linha -> new DistritoOrigemDTO(
                                origem.name(),
                                toLong(linha[0]),
                                toLong(linha[1]),
                                toStr(linha[2]),
                                toStr(linha[3]),
                                toStr(linha[4]),
                                toLong(linha[5]),
                                toLong(linha[6]),
                                toStr(linha[7]),
                                toLong(linha[8]),
                                toStr(linha[9]),
                                toLong(linha[10])
                        ))
                        .toList();

        return new PageImpl<>(lista, pageable, total == null ? 0L : total);
    }

    /*
     * Todos os distritos pendentes com situação UNICA.
     * Retorna trios [cidade_origem, distrito_origem, id_sugerido].
     */
    public List<Long[]> listarUnicos(OrigemDistrito origem) {

        DistritoUnificadoFilter filter = new DistritoUnificadoFilter();

        String sql = " select c.cidade_origem, c.distrito_origem, c.id_sugerido"
                + " from (" + sqlClassificacao(origem, filter) + ") c"
                + " where c.situacao = 'UNICA'"
                + " order by c.cidade_origem, c.distrito_origem";

        Query query = manager.createNativeQuery(sql);

        aplicarParametros(origem, filter, query);

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = query.getResultList();

        List<Long[]> resultado = new ArrayList<>();

        for (Object[] linha : linhas) {
            resultado.add(new Long[]{
                    toLong(linha[0]),
                    toLong(linha[1]),
                    toLong(linha[2])
            });
        }

        return resultado;
    }

    /**********************************************************************
     * RESUMO
     **********************************************************************/

    public DistritoUnificadoResumoDTO buscarResumo(OrigemDistrito origem) {

        String filtroValidos =
                " where nvl(o.cidade, 0) > 0 and nvl(o.distrito, 0) > 0";

        Long totalOrigem = count(
                "select count(*) from " + origem.getTabelaDistritosCompleta()
                        + " o" + filtroValidos,
                null
        );

        Long totalVinculados = count(
                "select count(*) from " + origem.getTabelaDistritosCompleta()
                        + " o" + filtroValidos
                        + " and exists (select 1 from " + TABELA_UNIFICADO
                        + " x where x." + origem.getColunaCidade() + " = o.cidade"
                        + " and x." + origem.getColunaDistrito() + " = o.distrito)",
                null
        );

        Long totalDuplicados = count(
                "select count(*) from " + TABELA_CTRL
                        + " k where k.origem = :origem and k.status = '"
                        + STATUS_DUPLICADO + "'",
                origem
        );

        Long totalNaoVincular = count(
                "select count(*) from " + TABELA_CTRL
                        + " k where k.origem = :origem and k.status = '"
                        + STATUS_NAO_VINCULAR + "'",
                origem
        );

        DistritoUnificadoFilter filter = new DistritoUnificadoFilter();

        String sql = " select c.situacao, count(*)"
                + " from (" + sqlClassificacao(origem, filter) + ") c"
                + " group by c.situacao";

        Query query = manager.createNativeQuery(sql);

        aplicarParametros(origem, filter, query);

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = query.getResultList();

        Map<String, Long> porSituacao = new HashMap<>();

        for (Object[] linha : linhas) {
            porSituacao.put(toStr(linha[0]), toLong(linha[1]));
        }

        long cidadeNaoUnificada = porSituacao.getOrDefault("CIDADE_NAO_UNIFICADA", 0L);
        long unicos = porSituacao.getOrDefault("UNICA", 0L);
        long duplicados = porSituacao.getOrDefault("DUPLICADA", 0L);
        long ambiguos = porSituacao.getOrDefault("AMBIGUA", 0L);
        long semCorrespondencia = porSituacao.getOrDefault("SEM_CORRESPONDENCIA", 0L);

        return new DistritoUnificadoResumoDTO(
                origem.name(),
                origem.getDescricao(),
                totalOrigem,
                totalVinculados,
                totalDuplicados,
                totalNaoVincular,
                cidadeNaoUnificada + unicos + duplicados + ambiguos + semCorrespondencia,
                cidadeNaoUnificada,
                unicos,
                duplicados,
                ambiguos,
                semCorrespondencia
        );
    }

    /**********************************************************************
     * CONSULTAS UNITÁRIAS
     **********************************************************************/

    public DistritoOrigemDados buscarDistritoOrigem(
            OrigemDistrito origem,
            Long cidade,
            Long distrito) {

        String sql = " select o.cidade,"
                + " o.distrito,"
                + " o.nome,"
                + " " + sqlUfCidadeOrigem(origem) + ","
                + " " + sqlNomeCidadeOrigem(origem) + ","
                + " " + origem.getExpressaoDistancia() + ","
                + " " + origem.getExpressaoCep() + ","
                + " " + FN_NORMALIZA + "(o.nome),"
                + " cc.cidade_correios,"
                + " nvl(cc.qtd_cidade, 0)"
                + " from " + origem.getTabelaDistritosCompleta() + " o"
                + " left join (" + sqlDeParaCidade(origem) + ") cc"
                + "   on cc.cod = o.cidade"
                + " where o.cidade = :cidade"
                + "   and o.distrito = :distrito"
                + "   and rownum = 1";

        @SuppressWarnings("unchecked")
        List<Object[]> linhas =
                manager.createNativeQuery(sql)
                        .setParameter("origem", origem.name())
                        .setParameter("cidade", cidade)
                        .setParameter("distrito", distrito)
                        .getResultList();

        if (linhas == null || linhas.isEmpty()) {
            return null;
        }

        Object[] linha = linhas.get(0);

        Long qtdCidades = toLong(linha[9]);

        return new DistritoOrigemDados(
                toLong(linha[0]),
                toLong(linha[1]),
                toStr(linha[2]),
                toStr(linha[3]),
                toStr(linha[4]),
                toBigDecimal(linha[5]),
                toLong(linha[6]),
                toStr(linha[7]),
                qtdCidades != null && qtdCidades == 1L ? toLong(linha[8]) : null,
                qtdCidades == null ? 0L : qtdCidades
        );
    }

    public boolean origemJaTratada(
            OrigemDistrito origem,
            Long cidade,
            Long distrito) {

        Number vinculado =
                (Number) manager.createNativeQuery(
                                "select count(*) from " + TABELA_UNIFICADO
                                        + " x where x." + origem.getColunaCidade() + " = :cidade"
                                        + " and x." + origem.getColunaDistrito() + " = :distrito"
                        )
                        .setParameter("cidade", cidade)
                        .setParameter("distrito", distrito)
                        .getSingleResult();

        if (vinculado != null && vinculado.longValue() > 0L) {
            return true;
        }

        Number controle =
                (Number) manager.createNativeQuery(
                                "select count(*) from " + TABELA_CTRL
                                        + " k where k.origem = :origem"
                                        + " and k.cidade_origem = :cidade"
                                        + " and k.distrito_origem = :distrito"
                        )
                        .setParameter("origem", origem.name())
                        .setParameter("cidade", cidade)
                        .setParameter("distrito", distrito)
                        .getSingleResult();

        return controle != null && controle.longValue() > 0L;
    }

    /*
     * Candidatos dentro da cidade dos Correios do distrito:
     *
     * - padrão: mesmo nome normalizado;
     * - pesquisa: parte do nome normalizado;
     * - todos: todos os distritos da cidade.
     *
     * Limitado a 200 linhas.
     */
    public List<DistritoCandidatoDTO> buscarCandidatos(
            OrigemDistrito origem,
            DistritoOrigemDados dados,
            String pesquisa,
            boolean todos) {

        if (dados.cidadeCorreios() == null) {
            return new ArrayList<>();
        }

        boolean manual = StringUtils.hasText(pesquisa);

        StringBuilder sql = new StringBuilder();

        sql.append("select * from (")
                .append(" select du.id,")
                .append(" du.cidade_correios,")
                .append(" du.distrito_correios,")
                .append(" du.nome,")
                .append(" upper(trim(du.estado)),")
                .append(" du.cep,")
                .append(" greatest(nvl(du.").append(origem.getColunaCidade()).append(", 0),")
                .append("          nvl(du.").append(origem.getColunaDistrito()).append(", 0)),")
                .append(" du.status_cor,")
                .append(" case")
                .append("   when ").append(FN_NORMALIZA).append("(du.nome) = :nomeNorm then 'NOME'")
                .append(manual ? "   else 'PESQUISA'" : "   else 'CIDADE'")
                .append(" end")
                .append(" from ").append(TABELA_UNIFICADO).append(" du")
                .append(" where du.cidade_correios = :cidadeCorreios");

        if (manual) {
            sql.append(" and ").append(FN_NORMALIZA)
                    .append("(du.nome) like '%' || ").append(FN_NORMALIZA)
                    .append("(:pesquisa) || '%'");
        } else if (!todos) {
            sql.append(" and ").append(FN_NORMALIZA)
                    .append("(du.nome) = :nomeNorm");
        }

        sql.append(" order by du.nome, du.id")
                .append(") where rownum <= 200");

        Query query =
                manager.createNativeQuery(sql.toString())
                        .setParameter("nomeNorm", nvl(dados.nomeNormalizado()))
                        .setParameter("cidadeCorreios", dados.cidadeCorreios());

        if (manual) {
            query.setParameter("pesquisa", pesquisa.trim());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = query.getResultList();

        return linhas.stream()
                .map(linha -> new DistritoCandidatoDTO(
                        toLong(linha[0]),
                        toLong(linha[1]),
                        toLong(linha[2]),
                        toStr(linha[3]),
                        toStr(linha[4]),
                        toLong(linha[5]),
                        toLong(linha[6]),
                        toStr(linha[7]),
                        toStr(linha[8])
                ))
                .toList();
    }

    /*
     * Cidade dos Correios do distrito unificado.
     * Retorna -1 quando o ID não existe.
     */
    public Long buscarCidadeCorreiosDoUnificado(Long idUnificado) {

        @SuppressWarnings("unchecked")
        List<Object> linhas =
                manager.createNativeQuery(
                                "select nvl(du.cidade_correios, 0) from "
                                        + TABELA_UNIFICADO
                                        + " du where du.id = :id"
                        )
                        .setParameter("id", idUnificado)
                        .getResultList();

        if (linhas == null || linhas.isEmpty()) {
            return -1L;
        }

        return toLong(linhas.get(0));
    }

    /**********************************************************************
     * GRAVAÇÕES
     **********************************************************************/

    /*
     * Grava cidade + distrito da origem no distrito unificado.
     *
     * Só grava quando o par da origem ainda está vazio (null ou 0).
     * Completa DISTANCIA e CEP somente quando estiverem vazios.
     *
     * Retorna 0 quando o distrito unificado já possui outro código desta origem.
     */
    public int vincularDistrito(
            OrigemDistrito origem,
            Long idUnificado,
            DistritoOrigemDados dados) {

        String colunaCidade = origem.getColunaCidade();
        String colunaDistrito = origem.getColunaDistrito();

        String sql = "update " + TABELA_UNIFICADO + " du"
                + " set du." + colunaCidade + " = :cidade,"
                + " du." + colunaDistrito + " = :distrito,"
                + " du.distancia = case when nvl(du.distancia, 0) = 0 and :distancia > 0"
                + "               then :distancia else du.distancia end,"
                + " du.cep = case when nvl(du.cep, 0) = 0 and :cep > 0"
                + "               then :cep else du.cep end,"
                + " du.data_alteracao = sysdate"
                + " where du.id = :id"
                + "   and nvl(du." + colunaCidade + ", 0) = 0"
                + "   and nvl(du." + colunaDistrito + ", 0) = 0";

        return manager.createNativeQuery(sql)
                .setParameter("cidade", dados.cidade())
                .setParameter("distrito", dados.distrito())
                .setParameter("distancia", nvl(dados.distancia()))
                .setParameter("cep", nvl(dados.cep()))
                .setParameter("id", idUnificado)
                .executeUpdate();
    }

    /*
     * status D: código duplicado da origem, vinculado a idUnificado;
     * status N: não vincular.
     */
    public void registrarControle(
            OrigemDistrito origem,
            Long cidadeOrigem,
            Long distritoOrigem,
            String status,
            Long idUnificado,
            String motivo,
            String usuario) {

        manager.createNativeQuery("""
            merge into DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO_CTRL destino
            using
            (
                select :origem as origem,
                       :cidadeOrigem as cidade_origem,
                       :distritoOrigem as distrito_origem
                  from dual
            ) origem
               on
               (
                   destino.ORIGEM = origem.origem
                   and destino.CIDADE_ORIGEM = origem.cidade_origem
                   and destino.DISTRITO_ORIGEM = origem.distrito_origem
               )
            when matched then
                update set
                    destino.STATUS = :status,
                    destino.ID_UNIFICADO = :idUnificado,
                    destino.MOTIVO = :motivo,
                    destino.USUARIO = :usuario,
                    destino.DATA_REGISTRO = sysdate
            when not matched then
                insert
                (
                    ID,
                    ORIGEM,
                    CIDADE_ORIGEM,
                    DISTRITO_ORIGEM,
                    STATUS,
                    ID_UNIFICADO,
                    MOTIVO,
                    USUARIO,
                    DATA_REGISTRO
                )
                values
                (
                    DBO_UNI_PESSOAS.SEQ_DISTRITOS_UNIFICADO_CTRL.nextval,
                    :origem,
                    :cidadeOrigem,
                    :distritoOrigem,
                    :status,
                    :idUnificado,
                    :motivo,
                    :usuario,
                    sysdate
                )
        """)
                .setParameter("origem", origem.name())
                .setParameter("cidadeOrigem", cidadeOrigem)
                .setParameter("distritoOrigem", distritoOrigem)
                .setParameter("status", status)
                .setParameter("idUnificado", idUnificado)
                .setParameter("motivo", motivo)
                .setParameter("usuario", usuario)
                .executeUpdate();
    }

    /*
     * Cria o distrito que não existe na base dos Correios:
     *
     * 1. DBO_CORREIOS.DISTRITOS na cidade dos Correios, STATUS = 'N',
     *    TIPO_LOCALIDADE = 'D', código pela sequence (nunca por MAX + 10000);
     * 2. DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO com o código desta origem
     *    e zero nas demais origens.
     *
     * Retorna o ID gerado em DISTRITOS_UNIFICADO.
     */
    public Long criarDistrito(
            OrigemDistrito origem,
            DistritoOrigemDados dados) {

        Long distritoCorreios = nextVal(SEQ_CORREIOS);

        manager.createNativeQuery(
                        "insert into " + TABELA_CORREIOS
                                + " (CIDADE, DISTRITO, NOME, ESTADO, TIPO_LOCALIDADE, STATUS)"
                                + " values (:cidade, :distrito, upper(trim(:nome)), :uf, 'D', 'N')"
                )
                .setParameter("cidade", dados.cidadeCorreios())
                .setParameter("distrito", distritoCorreios)
                .setParameter("nome", dados.nome())
                .setParameter("uf", nvl(dados.uf()))
                .executeUpdate();

        Long idUnificado = nextVal(SEQ_UNIFICADO);

        StringBuilder colunasOrigem = new StringBuilder();
        StringBuilder valoresOrigem = new StringBuilder();

        for (OrigemDistrito item : OrigemDistrito.values()) {

            colunasOrigem.append(item.getColunaCidade()).append(", ")
                    .append(item.getColunaDistrito()).append(", ");

            if (item == origem) {
                valoresOrigem.append(":cidadeOrigem, :distritoOrigem, ");
            } else {
                valoresOrigem.append("0, 0, ");
            }
        }

        String sql = "insert into " + TABELA_UNIFICADO
                + " (ID, CIDADE_CORREIOS, DISTRITO_CORREIOS, "
                + colunasOrigem
                + "NOME, ESTADO, DISTANCIA, CEP, CIDADE_INEP, DISTRITO_INEP,"
                + " DATA_ALTERACAO, USUARIO_ALTERACAO, STATUS_COR)"
                + " values (:id, :cidadeCorreios, :distritoCorreios, "
                + valoresOrigem
                + "upper(trim(:nome)), :uf, nullif(:distancia, 0), nullif(:cep, 0), 0, 0,"
                + " sysdate, null, :statusCor)";

        manager.createNativeQuery(sql)
                .setParameter("id", idUnificado)
                .setParameter("cidadeCorreios", dados.cidadeCorreios())
                .setParameter("distritoCorreios", distritoCorreios)
                .setParameter("cidadeOrigem", dados.cidade())
                .setParameter("distritoOrigem", dados.distrito())
                .setParameter("nome", dados.nome())
                .setParameter("uf", nvl(dados.uf()))
                .setParameter("distancia", nvl(dados.distancia()))
                .setParameter("cep", nvl(dados.cep()))
                .setParameter("statusCor", origem.getStatusCor())
                .executeUpdate();

        return idUnificado;
    }

    /**********************************************************************
     * SQL BASE
     **********************************************************************/

    /*
     * De-para: código da cidade na origem -> cidade dos Correios.
     *
     * Considera o código gravado em CIDADES_UNIFICADO e também
     * os códigos duplicados registrados em CIDADES_UNIFICADO_CTRL (status D).
     *
     * qtd_cidade > 1 indica que a cidade da origem aponta para mais
     * de uma cidade dos Correios (precisa ser corrigido na tela de cidades).
     */
    private String sqlDeParaCidade(OrigemDistrito origem) {

        String coluna = origem.getColunaCidadeUnificado();

        return " select x.cod,"
                + " count(distinct cu.cidade_correios) as qtd_cidade,"
                + " min(cu.cidade_correios) as cidade_correios"
                + " from ("
                + "   select cu1." + coluna + " as cod, cu1.id"
                + "     from " + TABELA_CIDADES_UNIFICADO + " cu1"
                + "    where nvl(cu1." + coluna + ", 0) > 0"
                + "   union"
                + "   select ct.cidade_origem as cod, ct.id_unificado as id"
                + "     from " + TABELA_CIDADES_CTRL + " ct"
                + "    where ct.origem = :origem"
                + "      and ct.status = 'D'"
                + "      and ct.id_unificado is not null"
                + " ) x"
                + " join " + TABELA_CIDADES_UNIFICADO + " cu"
                + "   on cu.id = x.id"
                + " where nvl(cu.cidade_correios, 0) > 0"
                + " group by x.cod";
    }

    private String sqlUfCidadeOrigem(OrigemDistrito origem) {
        return "(select max(upper(trim(c.estado))) from "
                + origem.getTabelaCidadesCompleta()
                + " c where c.cidade = o.cidade)";
    }

    private String sqlNomeCidadeOrigem(OrigemDistrito origem) {
        return "(select max(c.nome) from "
                + origem.getTabelaCidadesCompleta()
                + " c where c.cidade = o.cidade)";
    }

    /*
     * Distritos da origem ainda não tratados.
     */
    private String sqlOrigemPendente(
            OrigemDistrito origem,
            DistritoUnificadoFilter filter) {

        StringBuilder sql = new StringBuilder();

        sql.append(" select t.* from (")
                .append(" select o.cidade as cidade_origem,")
                .append(" o.distrito as distrito_origem,")
                .append(" o.nome as nome,")
                .append(" ").append(sqlUfCidadeOrigem(origem)).append(" as uf,")
                .append(" ").append(sqlNomeCidadeOrigem(origem)).append(" as nome_cidade,")
                .append(" ").append(FN_NORMALIZA).append("(o.nome) as nome_norm,")
                .append(" case when cc.qtd_cidade = 1 then cc.cidade_correios end as cidade_correios,")
                .append(" nvl(cc.qtd_cidade, 0) as qtd_cidade")
                .append(" from ").append(origem.getTabelaDistritosCompleta()).append(" o")
                .append(" left join (").append(sqlDeParaCidade(origem)).append(") cc")
                .append("   on cc.cod = o.cidade")
                .append(" where nvl(o.cidade, 0) > 0")
                .append("   and nvl(o.distrito, 0) > 0")
                .append("   and not exists (select 1 from ").append(TABELA_UNIFICADO)
                .append("        x where x.").append(origem.getColunaCidade()).append(" = o.cidade")
                .append("        and x.").append(origem.getColunaDistrito()).append(" = o.distrito)")
                .append("   and not exists (select 1 from ").append(TABELA_CTRL)
                .append("        k where k.origem = :origem")
                .append("        and k.cidade_origem = o.cidade")
                .append("        and k.distrito_origem = o.distrito)");

        if (filter.getCidade() != null) {
            sql.append(" and o.cidade = :cidade");
        }

        if (filter.getDistrito() != null) {
            sql.append(" and o.distrito = :distrito");
        }

        if (StringUtils.hasText(filter.getNome())) {
            sql.append(" and upper(o.nome) like :nome");
        }

        sql.append(" ) t where 1 = 1");

        if (StringUtils.hasText(filter.getNomeCidade())) {
            sql.append(" and upper(t.nome_cidade) like :nomeCidade");
        }

        if (StringUtils.hasText(filter.getUf())) {
            sql.append(" and t.uf = :uf");
        }

        return sql.toString();
    }

    /*
     * Distritos unificados com o nome normalizado calculado uma vez.
     */
    private String sqlUnificadoNormalizado(OrigemDistrito origem) {

        return " select du.id,"
                + " du.cidade_correios,"
                + " " + FN_NORMALIZA + "(du.nome) as nome_norm,"
                + " greatest(nvl(du." + origem.getColunaCidade() + ", 0),"
                + "          nvl(du." + origem.getColunaDistrito() + ", 0)) as cod_atual"
                + " from " + TABELA_UNIFICADO + " du";
    }

    private String sqlClassificacao(
            OrigemDistrito origem,
            DistritoUnificadoFilter filter) {

        String pendentes = sqlOrigemPendente(origem, filter);
        String unificado = sqlUnificadoNormalizado(origem);

        return " select po.cidade_origem,"
                + " po.distrito_origem,"
                + " po.nome,"
                + " po.uf,"
                + " po.nome_cidade,"
                + " po.cidade_correios,"
                + " nvl(m.qtd, 0) as qtd_candidatos,"
                + " case when nvl(m.qtd, 0) = 1 then m.id_min end as id_sugerido,"
                + " case"
                + "   when po.qtd_cidade <> 1 then 'CIDADE_NAO_UNIFICADA'"
                + "   when nvl(m.qtd, 0) = 0 then 'SEM_CORRESPONDENCIA'"
                + "   when m.qtd > 1 then 'AMBIGUA'"
                + "   when m.qtd_ocupado > 0 then 'DUPLICADA'"
                + "   else 'UNICA'"
                + " end as situacao"
                + " from (" + pendentes + ") po"
                + " left join ("
                + "   select pn.cidade_origem,"
                + "          pn.distrito_origem,"
                + "          count(*) as qtd,"
                + "          min(un.id) as id_min,"
                + "          sum(case when un.cod_atual > 0 then 1 else 0 end) as qtd_ocupado"
                + "     from (" + pendentes + ") pn"
                + "     join (" + unificado + ") un"
                + "       on un.cidade_correios = pn.cidade_correios"
                + "      and un.nome_norm = pn.nome_norm"
                + "    where pn.qtd_cidade = 1"
                + "    group by pn.cidade_origem, pn.distrito_origem"
                + " ) m"
                + " on m.cidade_origem = po.cidade_origem"
                + " and m.distrito_origem = po.distrito_origem";
    }

    private void aplicarParametros(
            OrigemDistrito origem,
            DistritoUnificadoFilter filter,
            Query query) {

        query.setParameter("origem", origem.name());

        if (filter.getCidade() != null) {
            query.setParameter("cidade", filter.getCidade());
        }

        if (filter.getDistrito() != null) {
            query.setParameter("distrito", filter.getDistrito());
        }

        if (StringUtils.hasText(filter.getNome())) {
            query.setParameter(
                    "nome",
                    "%" + filter.getNome().trim().toUpperCase() + "%"
            );
        }

        if (StringUtils.hasText(filter.getNomeCidade())) {
            query.setParameter(
                    "nomeCidade",
                    "%" + filter.getNomeCidade().trim().toUpperCase() + "%"
            );
        }

        if (StringUtils.hasText(filter.getUf())) {
            query.setParameter("uf", filter.getUf().trim().toUpperCase());
        }
    }

    /**********************************************************************
     * UTILITÁRIOS
     **********************************************************************/

    private Long count(String sql, OrigemDistrito origem) {

        Query query = manager.createNativeQuery(sql);

        if (origem != null) {
            query.setParameter("origem", origem.name());
        }

        Object result = query.getSingleResult();

        return result == null ? 0L : ((Number) result).longValue();
    }

    private Long nextVal(String sequence) {

        Object result =
                manager.createNativeQuery("select " + sequence + ".nextval from dual")
                        .getSingleResult();

        return ((Number) result).longValue();
    }

    private static Long toLong(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof Number numero) {
            return numero.longValue();
        }

        String texto = valor.toString().trim();

        return texto.isEmpty() ? null : Long.valueOf(texto);
    }

    private static BigDecimal toBigDecimal(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof BigDecimal numero) {
            return numero;
        }

        if (valor instanceof Number numero) {
            return BigDecimal.valueOf(numero.doubleValue());
        }

        String texto = valor.toString().trim();

        return texto.isEmpty() ? null : new BigDecimal(texto);
    }

    private static String toStr(Object valor) {
        return valor == null ? null : valor.toString().trim();
    }

    private static Long nvl(Long valor) {
        return valor == null ? 0L : valor;
    }

    private static BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static String nvl(String valor) {
        return valor == null ? "" : valor;
    }
}
