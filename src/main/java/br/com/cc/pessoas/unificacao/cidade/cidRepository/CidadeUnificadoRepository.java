package br.com.cc.pessoas.unificacao.cidade.cidRepository;

import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeCandidatoDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeOrigemDTO;
import br.com.cc.pessoas.unificacao.cidade.cidDto.CidadeUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import br.com.cc.pessoas.unificacao.cidade.cidFilter.CidadeUnificadoFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Consultas e gravações da unificação de cidades.
 *
 * Tabelas envolvidas:
 *
 * - DBO_UNI_PESSOAS.<ORIGEM>_CIDADES      (sinônimos, somente leitura)
 * - DBO_UNI_PESSOAS.CIDADES_UNIFICADO     (de-para por sistema)
 * - DBO_UNI_PESSOAS.CIDADES_UNIFICADO_CTRL (duplicados e "não vincular")
 * - DBO_CORREIOS.CIDADES                  (somente inclusão de cidade nova, STATUS = 'N')
 *
 * Todo o SQL é compatível com Oracle 11g:
 * sem WITH, sem FETCH FIRST e com paginação por ROWNUM.
 */
@Repository
public class CidadeUnificadoRepository {

    private static final String SCHEMA =
            OrigemCidade.SCHEMA_UNIFICADO;

    private static final String TABELA_UNIFICADO =
            SCHEMA + ".CIDADES_UNIFICADO";

    private static final String TABELA_CTRL =
            SCHEMA + ".CIDADES_UNIFICADO_CTRL";

    private static final String FN_NORMALIZA =
            SCHEMA + ".FN_NORMALIZA_CIDADE";

    private static final String TABELA_CORREIOS =
            "DBO_CORREIOS.CIDADES";

    private static final String SEQ_CORREIOS =
            "DBO_CORREIOS.SEQ_CIDADES_FICTICIAS";

    private static final String SEQ_UNIFICADO =
            SCHEMA + ".SEQ_CIDADES_UNIFICADO";

    private static final String SEQ_CTRL =
            SCHEMA + ".SEQ_CIDADES_UNIFICADO_CTRL";

    public static final String STATUS_DUPLICADO = "D";
    public static final String STATUS_NAO_VINCULAR = "N";

    @PersistenceContext
    private EntityManager manager;

    /*
     * Dados completos de uma cidade da origem,
     * usados nas ações de vincular e criar.
     */
    public record CidadeOrigemDados(
            Long cidade,
            String nome,
            String uf,
            Long cep,
            String ddd,
            Long codigoIbge,
            Long cidadeSicom,
            Long cidadeInep,
            String nomeNormalizado
    ) {
    }

    /**********************************************************************
     * LISTAGEM
     **********************************************************************/

    public Page<CidadeOrigemDTO> filtrarPendentes(
            OrigemCidade origem,
            CidadeUnificadoFilter filter,
            Pageable pageable) {

        StringBuilder sqlClassificado =
                new StringBuilder();

        sqlClassificado.append("""
            select c.*,
                   count(*) over() as total_registros
              from (
            """);

        sqlClassificado.append(
                sqlClassificacao(origem, filter)
        );

        sqlClassificado.append("""
                   ) c
             where 1 = 1
            """);

        if (StringUtils.hasText(filter.getSituacao())) {
            sqlClassificado.append(
                    " and c.situacao = :situacao "
            );
        }

        sqlClassificado.append(
                " order by c.nome, c.cidade_origem "
        );

        /*
         * Paginação por ROWNUM (Oracle 11g).
         * A sugestão é buscada somente para as linhas da página.
         */
        String sql = " select p.cidade_origem,"
                + " p.nome,"
                + " p.uf,"
                + " p.cep,"
                + " p.ddd,"
                + " p.ibge,"
                + " p.qtd_candidatos,"
                + " p.situacao,"
                + " p.id_sugerido,"
                + " s.nome as nome_sugerido,"
                + " s.cidade_correios as cidade_correios_sugerida,"
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

        Query query =
                manager.createNativeQuery(sql);

        aplicarParametros(origem, filter, query);

        if (StringUtils.hasText(filter.getSituacao())) {
            query.setParameter(
                    "situacao",
                    filter.getSituacao().trim().toUpperCase()
            );
        }

        long inicio = pageable.getOffset();

        query.setParameter("inicio", inicio);
        query.setParameter(
                "fim",
                inicio + pageable.getPageSize()
        );

        @SuppressWarnings("unchecked")
        List<Object[]> linhas =
                query.getResultList();

        if (linhas == null || linhas.isEmpty()) {
            return new PageImpl<>(
                    new ArrayList<>(),
                    pageable,
                    0L
            );
        }

        Long total = toLong(linhas.get(0)[11]);

        List<CidadeOrigemDTO> lista =
                linhas.stream()
                        .map(linha -> new CidadeOrigemDTO(
                                origem.name(),
                                toLong(linha[0]),
                                toStr(linha[1]),
                                toStr(linha[2]),
                                toLong(linha[3]),
                                toStr(linha[4]),
                                toLong(linha[5]),
                                toLong(linha[6]),
                                toStr(linha[7]),
                                toLong(linha[8]),
                                toStr(linha[9]),
                                toLong(linha[10])
                        ))
                        .toList();

        return new PageImpl<>(
                lista,
                pageable,
                total == null ? 0L : total
        );
    }

    /*
     * Lista todas as cidades pendentes com situação UNICA.
     * Usada pela vinculação em lote.
     *
     * Retorna pares [cidade_origem, id_sugerido].
     */
    public List<Long[]> listarUnicas(OrigemCidade origem) {

        CidadeUnificadoFilter filter =
                new CidadeUnificadoFilter();

        String sql = """
            select c.cidade_origem,
                   c.id_sugerido
              from (
            """ + sqlClassificacao(origem, filter) + """
                   ) c
             where c.situacao = 'UNICA'
             order by c.nome, c.cidade_origem
            """;

        Query query =
                manager.createNativeQuery(sql);

        aplicarParametros(origem, filter, query);

        @SuppressWarnings("unchecked")
        List<Object[]> linhas =
                query.getResultList();

        List<Long[]> resultado =
                new ArrayList<>();

        for (Object[] linha : linhas) {
            resultado.add(new Long[]{
                    toLong(linha[0]),
                    toLong(linha[1])
            });
        }

        return resultado;
    }

    /**********************************************************************
     * RESUMO
     **********************************************************************/

    public CidadeUnificadoResumoDTO buscarResumo(
            OrigemCidade origem) {

        Long totalOrigem = count(
                "select count(*) from "
                        + origem.getTabelaOrigemCompleta()
                        + " o where nvl(o.cidade, 0) > 0",
                null
        );

        Long totalVinculadas = count(
                "select count(*) from "
                        + origem.getTabelaOrigemCompleta()
                        + " o where nvl(o.cidade, 0) > 0"
                        + " and exists (select 1 from "
                        + TABELA_UNIFICADO
                        + " x where x."
                        + origem.getColunaUnificado()
                        + " = o.cidade)",
                null
        );

        Long totalDuplicadasRegistradas = count(
                "select count(*) from "
                        + TABELA_CTRL
                        + " c where c.origem = :origem"
                        + " and c.status = '"
                        + STATUS_DUPLICADO + "'",
                origem
        );

        Long totalNaoVincular = count(
                "select count(*) from "
                        + TABELA_CTRL
                        + " c where c.origem = :origem"
                        + " and c.status = '"
                        + STATUS_NAO_VINCULAR + "'",
                origem
        );

        CidadeUnificadoFilter filter =
                new CidadeUnificadoFilter();

        String sql = """
            select c.situacao,
                   count(*)
              from (
            """ + sqlClassificacao(origem, filter) + """
                   ) c
             group by c.situacao
            """;

        Query query =
                manager.createNativeQuery(sql);

        aplicarParametros(origem, filter, query);

        @SuppressWarnings("unchecked")
        List<Object[]> linhas =
                query.getResultList();

        Map<String, Long> porSituacao =
                new HashMap<>();

        for (Object[] linha : linhas) {
            porSituacao.put(
                    toStr(linha[0]),
                    toLong(linha[1])
            );
        }

        long unicas = porSituacao.getOrDefault("UNICA", 0L);
        long duplicadas = porSituacao.getOrDefault("DUPLICADA", 0L);
        long ambiguas = porSituacao.getOrDefault("AMBIGUA", 0L);
        long semCorrespondencia =
                porSituacao.getOrDefault("SEM_CORRESPONDENCIA", 0L);

        return new CidadeUnificadoResumoDTO(
                origem.name(),
                origem.getDescricao(),
                totalOrigem,
                totalVinculadas,
                totalDuplicadasRegistradas,
                totalNaoVincular,
                unicas + duplicadas + ambiguas + semCorrespondencia,
                unicas,
                duplicadas,
                ambiguas,
                semCorrespondencia
        );
    }

    /**********************************************************************
     * CONSULTAS UNITÁRIAS
     **********************************************************************/

    public CidadeOrigemDados buscarCidadeOrigem(
            OrigemCidade origem,
            Long cidade) {

        String sql = """
            select o.cidade,
                   o.nome,
                   upper(trim(o.estado)),
            """
                + origem.getExpressaoCep() + ", "
                + origem.getExpressaoDdd() + ", "
                + origem.getExpressaoIbge() + ", "
                + origem.getExpressaoSicom() + ", "
                + origem.getExpressaoInep() + ", "
                + FN_NORMALIZA + "(o.nome)"
                + " from " + origem.getTabelaOrigemCompleta() + " o"
                + " where o.cidade = :cidade"
                + "   and rownum = 1";

        @SuppressWarnings("unchecked")
        List<Object[]> linhas =
                manager.createNativeQuery(sql)
                        .setParameter("cidade", cidade)
                        .getResultList();

        if (linhas == null || linhas.isEmpty()) {
            return null;
        }

        Object[] linha = linhas.get(0);

        return new CidadeOrigemDados(
                toLong(linha[0]),
                toStr(linha[1]),
                toStr(linha[2]),
                toLong(linha[3]),
                toStr(linha[4]),
                toLong(linha[5]),
                toLong(linha[6]),
                toLong(linha[7]),
                toStr(linha[8])
        );
    }

    /*
     * Verifica se o código da origem já está em CIDADES_UNIFICADO
     * ou já possui decisão registrada em CIDADES_UNIFICADO_CTRL.
     */
    public boolean origemJaTratada(
            OrigemCidade origem,
            Long cidade) {

        Long vinculada = count(
                "select count(*) from "
                        + TABELA_UNIFICADO
                        + " x where x."
                        + origem.getColunaUnificado()
                        + " = " + Long.valueOf(cidade),
                null
        );

        if (vinculada > 0) {
            return true;
        }

        Number controle =
                (Number) manager.createNativeQuery(
                                "select count(*) from "
                                        + TABELA_CTRL
                                        + " c where c.origem = :origem"
                                        + " and c.cidade_origem = :cidade"
                        )
                        .setParameter("origem", origem.name())
                        .setParameter("cidade", cidade)
                        .getSingleResult();

        return controle != null && controle.longValue() > 0L;
    }

    /*
     * Candidatos automáticos: mesmo UF e (mesmo nome normalizado
     * ou mesmo código IBGE, quando a origem possui IBGE).
     *
     * Quando "pesquisa" é informada, faz a busca manual por parte
     * do nome normalizado no UF informado, limitada a 50 linhas.
     */
    public List<CidadeCandidatoDTO> buscarCandidatos(
            OrigemCidade origem,
            CidadeOrigemDados dados,
            String pesquisa,
            String ufPesquisa) {

        boolean manual = StringUtils.hasText(pesquisa);

        String uf =
                manual && StringUtils.hasText(ufPesquisa)
                        ? ufPesquisa.trim().toUpperCase()
                        : dados.uf();

        StringBuilder sql = new StringBuilder();

        sql.append("select * from (")
                .append(" select cu.id,")
                .append(" cu.cidade_correios,")
                .append(" cu.nome,")
                .append(" upper(trim(cu.estado)),")
                .append(" cu.cep,")
                .append(" cu.codigo_ibge,")
                .append(" nvl(cu.").append(origem.getColunaUnificado()).append(", 0),")
                .append(" cu.status_cor,")
                .append(" case")
                .append("   when ").append(FN_NORMALIZA).append("(cu.nome) = :nomeNorm then 'NOME'")
                .append("   when :ibge > 0 and cu.codigo_ibge = :ibge then 'IBGE'")
                .append("   else 'PESQUISA'")
                .append(" end")
                .append(" from ").append(TABELA_UNIFICADO).append(" cu")
                .append(" where upper(trim(cu.estado)) = :uf");

        if (manual) {
            sql.append(" and ")
                    .append(FN_NORMALIZA)
                    .append("(cu.nome) like '%' || ")
                    .append(FN_NORMALIZA)
                    .append("(:pesquisa) || '%'");
        } else {
            sql.append(" and (")
                    .append(FN_NORMALIZA)
                    .append("(cu.nome) = :nomeNorm")
                    .append(" or (:ibge > 0 and cu.codigo_ibge = :ibge))");
        }

        sql.append(" order by cu.nome, cu.id")
                .append(") where rownum <= 50");

        Query query =
                manager.createNativeQuery(sql.toString())
                        .setParameter("nomeNorm", nvl(dados.nomeNormalizado()))
                        .setParameter("ibge", nvl(dados.codigoIbge()))
                        .setParameter("uf", nvl(uf));

        if (manual) {
            query.setParameter("pesquisa", pesquisa.trim());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> linhas =
                query.getResultList();

        return linhas.stream()
                .map(linha -> new CidadeCandidatoDTO(
                        toLong(linha[0]),
                        toLong(linha[1]),
                        toStr(linha[2]),
                        toStr(linha[3]),
                        toLong(linha[4]),
                        toLong(linha[5]),
                        toLong(linha[6]),
                        toStr(linha[7]),
                        toStr(linha[8])
                ))
                .toList();
    }

    public String buscarUfUnificado(Long idUnificado) {

        @SuppressWarnings("unchecked")
        List<Object> linhas =
                manager.createNativeQuery(
                                "select upper(trim(cu.estado)) from "
                                        + TABELA_UNIFICADO
                                        + " cu where cu.id = :id"
                        )
                        .setParameter("id", idUnificado)
                        .getResultList();

        if (linhas == null || linhas.isEmpty()) {
            return null;
        }

        String uf = toStr(linhas.get(0));

        return uf == null ? "" : uf;
    }

    /**********************************************************************
     * GRAVAÇÕES
     **********************************************************************/

    /*
     * Grava o código da origem na cidade unificada.
     *
     * Só grava quando a coluna da origem ainda está vazia (null ou 0).
     * Aproveita para completar DDD, CEP, IBGE, SICOM e INEP
     * somente quando estiverem vazios. Nada é sobrescrito.
     *
     * Retorna a quantidade de linhas alteradas:
     * 0 indica que a cidade já possui outro código desta origem.
     */
    public int vincularCidade(
            OrigemCidade origem,
            Long idUnificado,
            CidadeOrigemDados dados) {

        String coluna = origem.getColunaUnificado();

        String sql = "update " + TABELA_UNIFICADO + " cu"
                + " set cu." + coluna + " = :cidade,"
                + " cu.ddd = case when trim(cu.ddd) is null or trim(cu.ddd) = '0'"
                + "               then nvl(:ddd, cu.ddd) else cu.ddd end,"
                + " cu.cep = case when nvl(cu.cep, 0) = 0 and :cep > 0"
                + "               then :cep else cu.cep end,"
                + " cu.codigo_ibge = case when nvl(cu.codigo_ibge, 0) = 0 and :ibge > 0"
                + "               then :ibge else cu.codigo_ibge end,"
                + " cu.cidade_sicom = case when nvl(cu.cidade_sicom, 0) = 0 and :sicom > 0"
                + "               then :sicom else cu.cidade_sicom end,"
                + " cu.cidade_inep = case when nvl(cu.cidade_inep, 0) = 0 and :inep > 0"
                + "               then :inep else cu.cidade_inep end,"
                + " cu.data_alteracao = sysdate"
                + " where cu.id = :id"
                + "   and nvl(cu." + coluna + ", 0) = 0";

        return manager.createNativeQuery(sql)
                .setParameter("cidade", dados.cidade())
                .setParameter("ddd", nvl(dados.ddd()))
                .setParameter("cep", nvl(dados.cep()))
                .setParameter("ibge", nvl(dados.codigoIbge()))
                .setParameter("sicom", nvl(dados.cidadeSicom()))
                .setParameter("inep", nvl(dados.cidadeInep()))
                .setParameter("id", idUnificado)
                .executeUpdate();
    }

    /*
     * Registra a decisão em CIDADES_UNIFICADO_CTRL.
     *
     * status D: código duplicado da origem, vinculado a idUnificado;
     * status N: não vincular.
     */
    public void registrarControle(
            OrigemCidade origem,
            Long cidadeOrigem,
            String status,
            Long idUnificado,
            String motivo,
            String usuario) {

        manager.createNativeQuery("""
            merge into DBO_UNI_PESSOAS.CIDADES_UNIFICADO_CTRL destino
            using
            (
                select :origem as origem,
                       :cidadeOrigem as cidade_origem
                  from dual
            ) origem
               on
               (
                   destino.ORIGEM = origem.origem
                   and destino.CIDADE_ORIGEM = origem.cidade_origem
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
                    STATUS,
                    ID_UNIFICADO,
                    MOTIVO,
                    USUARIO,
                    DATA_REGISTRO
                )
                values
                (
                    DBO_UNI_PESSOAS.SEQ_CIDADES_UNIFICADO_CTRL.nextval,
                    :origem,
                    :cidadeOrigem,
                    :status,
                    :idUnificado,
                    :motivo,
                    :usuario,
                    sysdate
                )
        """)
                .setParameter("origem", origem.name())
                .setParameter("cidadeOrigem", cidadeOrigem)
                .setParameter("status", status)
                .setParameter("idUnificado", idUnificado)
                .setParameter("motivo", motivo)
                .setParameter("usuario", usuario)
                .executeUpdate();
    }

    /*
     * Cria a cidade que não existe na base dos Correios:
     *
     * 1. DBO_CORREIOS.CIDADES com STATUS = 'N', código pela sequence
     *    (nunca por MAX + 1);
     * 2. DBO_UNI_PESSOAS.CIDADES_UNIFICADO com o código desta origem
     *    e zero nas demais origens.
     *
     * Retorna o ID gerado em CIDADES_UNIFICADO.
     */
    public Long criarCidade(
            OrigemCidade origem,
            CidadeOrigemDados dados) {

        Long codigoCorreios = nextVal(SEQ_CORREIOS);

        manager.createNativeQuery(
                        "insert into " + TABELA_CORREIOS
                                + " (CIDADE, NOME, ESTADO, CEP, CD_IBGE, STATUS)"
                                + " values (:cidade, :nome, :uf,"
                                + " nullif(:cep, 0), nullif(:ibge, 0), 'N')"
                )
                .setParameter("cidade", codigoCorreios)
                .setParameter("nome", trim(dados.nome()))
                .setParameter("uf", dados.uf())
                .setParameter("cep", nvl(dados.cep()))
                .setParameter("ibge", nvl(dados.codigoIbge()))
                .executeUpdate();

        Long idUnificado = nextVal(SEQ_UNIFICADO);

        StringBuilder colunasOrigem = new StringBuilder();
        StringBuilder valoresOrigem = new StringBuilder();

        for (OrigemCidade item : OrigemCidade.values()) {

            colunasOrigem.append(item.getColunaUnificado())
                    .append(", ");

            valoresOrigem.append(item == origem ? ":codigoOrigem" : "0")
                    .append(", ");
        }

        String sql = "insert into " + TABELA_UNIFICADO
                + " (ID, CIDADE_CORREIOS, "
                + colunasOrigem
                + "NOME, ESTADO, CEP, CIDADE_SICOM, CIDADE_INEP, CODIGO_IBGE, DDD,"
                + " DATA_ALTERACAO, USUARIO_ALTERACAO, STATUS_COR)"
                + " values (:id, :cidadeCorreios, "
                + valoresOrigem
                + ":nome, :uf, nullif(:cep, 0), :sicom, :inep, :ibge, :ddd,"
                + " sysdate, null, :statusCor)";

        manager.createNativeQuery(sql)
                .setParameter("id", idUnificado)
                .setParameter("cidadeCorreios", codigoCorreios)
                .setParameter("codigoOrigem", dados.cidade())
                .setParameter("nome", trim(dados.nome()))
                .setParameter("uf", dados.uf())
                .setParameter("cep", nvl(dados.cep()))
                .setParameter("sicom", nvl(dados.cidadeSicom()))
                .setParameter("inep", nvl(dados.cidadeInep()))
                .setParameter("ibge", nvl(dados.codigoIbge()))
                .setParameter("ddd", nvl(dados.ddd()))
                .setParameter("statusCor", origem.getStatusCor())
                .executeUpdate();

        return idUnificado;
    }

    /**********************************************************************
     * SQL BASE
     **********************************************************************/

    /*
     * Cidades da origem ainda não tratadas:
     * sem código em CIDADES_UNIFICADO e sem decisão em CIDADES_UNIFICADO_CTRL.
     */
    private String sqlOrigemPendente(
            OrigemCidade origem,
            CidadeUnificadoFilter filter) {

        StringBuilder sql = new StringBuilder();

        sql.append(" select o.cidade as cidade_origem,")
                .append(" o.nome as nome,")
                .append(" upper(trim(o.estado)) as uf,")
                .append(" ").append(origem.getExpressaoCep()).append(" as cep,")
                .append(" ").append(origem.getExpressaoDdd()).append(" as ddd,")
                .append(" nvl(").append(origem.getExpressaoIbge()).append(", 0) as ibge,")
                .append(" ").append(FN_NORMALIZA).append("(o.nome) as nome_norm")
                .append(" from ").append(origem.getTabelaOrigemCompleta()).append(" o")
                .append(" where nvl(o.cidade, 0) > 0")
                .append(" and not exists (select 1 from ").append(TABELA_UNIFICADO)
                .append(" x where x.").append(origem.getColunaUnificado())
                .append(" = o.cidade)")
                .append(" and not exists (select 1 from ").append(TABELA_CTRL)
                .append(" c where c.origem = :origem and c.cidade_origem = o.cidade)");

        if (filter.getCidade() != null) {
            sql.append(" and o.cidade = :cidade");
        }

        if (StringUtils.hasText(filter.getNome())) {
            sql.append(" and upper(o.nome) like :nome");
        }

        if (StringUtils.hasText(filter.getUf())) {
            sql.append(" and upper(trim(o.estado)) = :uf");
        }

        return sql.toString();
    }

    /*
     * Cidades unificadas com o nome normalizado calculado uma única vez.
     */
    private String sqlUnificadoNormalizado(
            OrigemCidade origem) {

        return " select cu.id,"
                + " " + FN_NORMALIZA + "(cu.nome) as nome_norm,"
                + " upper(trim(cu.estado)) as uf,"
                + " nvl(cu.codigo_ibge, 0) as codigo_ibge,"
                + " nvl(cu." + origem.getColunaUnificado() + ", 0) as cod_atual"
                + " from " + TABELA_UNIFICADO + " cu";
    }

    /*
     * Classificação de cada cidade pendente.
     *
     * Os dois critérios (nome + UF e IBGE + UF) são unidos com UNION
     * em vez de OR no JOIN, para o Oracle poder usar hash join
     * e chamar FN_NORMALIZA_CIDADE uma vez por linha.
     */
    private String sqlClassificacao(
            OrigemCidade origem,
            CidadeUnificadoFilter filter) {

        String pendentes = sqlOrigemPendente(origem, filter);
        String unificado = sqlUnificadoNormalizado(origem);

        return " select po.cidade_origem,"
                + " po.nome,"
                + " po.uf,"
                + " po.cep,"
                + " po.ddd,"
                + " po.ibge,"
                + " nvl(m.qtd, 0) as qtd_candidatos,"
                + " case when nvl(m.qtd, 0) = 1 then m.id_min end as id_sugerido,"
                + " case"
                + "   when nvl(m.qtd, 0) = 0 then 'SEM_CORRESPONDENCIA'"
                + "   when m.qtd > 1 then 'AMBIGUA'"
                + "   when m.qtd_ocupado > 0 then 'DUPLICADA'"
                + "   else 'UNICA'"
                + " end as situacao"
                + " from (" + pendentes + ") po"
                + " left join ("
                + "   select k.cidade_origem,"
                + "          count(*) as qtd,"
                + "          min(k.id) as id_min,"
                + "          sum(case when k.cod_atual > 0 then 1 else 0 end) as qtd_ocupado"
                + "     from ("
                + "       select pn.cidade_origem, un.id, un.cod_atual"
                + "         from (" + pendentes + ") pn"
                + "         join (" + unificado + ") un"
                + "           on un.uf = pn.uf"
                + "          and un.nome_norm = pn.nome_norm"
                + "       union"
                + "       select pi.cidade_origem, ui.id, ui.cod_atual"
                + "         from (" + pendentes + ") pi"
                + "         join (" + unificado + ") ui"
                + "           on ui.uf = pi.uf"
                + "          and ui.codigo_ibge = pi.ibge"
                + "        where pi.ibge > 0"
                + "     ) k"
                + "    group by k.cidade_origem"
                + " ) m"
                + " on m.cidade_origem = po.cidade_origem";
    }

    private void aplicarParametros(
            OrigemCidade origem,
            CidadeUnificadoFilter filter,
            Query query) {

        query.setParameter("origem", origem.name());

        if (filter.getCidade() != null) {
            query.setParameter("cidade", filter.getCidade());
        }

        if (StringUtils.hasText(filter.getNome())) {
            query.setParameter(
                    "nome",
                    "%" + filter.getNome().trim().toUpperCase() + "%"
            );
        }

        if (StringUtils.hasText(filter.getUf())) {
            query.setParameter(
                    "uf",
                    filter.getUf().trim().toUpperCase()
            );
        }
    }

    /**********************************************************************
     * UTILITÁRIOS
     **********************************************************************/

    private Long count(String sql, OrigemCidade origem) {

        Query query = manager.createNativeQuery(sql);

        if (origem != null) {
            query.setParameter("origem", origem.name());
        }

        Object result = query.getSingleResult();

        return result == null ? 0L : ((Number) result).longValue();
    }

    private Long nextVal(String sequence) {

        Object result =
                manager.createNativeQuery(
                                "select " + sequence + ".nextval from dual"
                        )
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

    private static String toStr(Object valor) {
        return valor == null ? null : valor.toString().trim();
    }

    private static String trim(String valor) {
        return valor == null ? null : valor.trim();
    }

    /*
     * Evita bind de null numérico/textual nas expressões
     * "case when :x > 0" (o Oracle trata '' como null).
     */
    private static Long nvl(Long valor) {
        return valor == null ? 0L : valor;
    }

    private static String nvl(String valor) {
        return valor == null ? "" : valor;
    }
}
