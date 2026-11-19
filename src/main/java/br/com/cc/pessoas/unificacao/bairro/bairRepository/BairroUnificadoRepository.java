package br.com.cc.pessoas.unificacao.bairro.bairRepository;

import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroCandidatoDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroOrigemDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.bairro.bairEnum.OrigemBairro;
import br.com.cc.pessoas.unificacao.bairro.bairFilter.BairroUnificadoFilter;
import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import br.com.cc.pessoas.unificacao.distrito.distEnum.OrigemDistrito;
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
 * Consultas e gravações da unificação de bairros.
 *
 * Tabelas envolvidas:
 *
 * - DBO_UNI_PESSOAS.<ORIGEM>_BAIRROS / _DISTRITOS / _CIDADES (sinônimos, leitura)
 * - DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO e DISTRITOS_UNIFICADO_CTRL (leitura:
 *   de-para de cidade + distrito da origem para cidade + distrito dos Correios)
 * - DBO_UNI_PESSOAS.BAIRROS_UNIFICADO      (de-para por sistema)
 * - DBO_UNI_PESSOAS.BAIRROS_UNIFICADO_CTRL (duplicados e "não vincular")
 * - DBO_CORREIOS.BAIRROS                   (somente inclusão de bairro novo, STATUS = 'N')
 *
 * Regra central: o bairro da origem só é comparado automaticamente com os
 * bairros do MESMO distrito dos Correios, obtido pelo de-para de distritos.
 * Na pesquisa manual, a busca é feita na cidade inteira.
 *
 * SQL compatível com Oracle 11g: sem WITH, sem FETCH FIRST, paginação por ROWNUM.
 */
@Repository
public class BairroUnificadoRepository {

    private static final String SCHEMA =
            OrigemCidade.SCHEMA_UNIFICADO;

    private static final String TABELA_DISTRITOS_UNIFICADO =
            SCHEMA + ".DISTRITOS_UNIFICADO";

    private static final String TABELA_DISTRITOS_CTRL =
            SCHEMA + ".DISTRITOS_UNIFICADO_CTRL";

    private static final String TABELA_UNIFICADO =
            SCHEMA + ".BAIRROS_UNIFICADO";

    private static final String TABELA_CTRL =
            SCHEMA + ".BAIRROS_UNIFICADO_CTRL";

    private static final String FN_NORMALIZA =
            SCHEMA + ".FN_NORMALIZA_CIDADE";

    private static final String TABELA_CORREIOS =
            "DBO_CORREIOS.BAIRROS";

    private static final String SEQ_CORREIOS =
            "DBO_CORREIOS.SEQ_BAIRROS_FICTICIOS";

    private static final String SEQ_UNIFICADO =
            SCHEMA + ".SEQ_BAIRROS_UNIFICADO";

    public static final String STATUS_DUPLICADO = "D";
    public static final String STATUS_NAO_VINCULAR = "N";

    private static final int LIMITE_CANDIDATOS = 500;

    @PersistenceContext
    private EntityManager manager;

    /*
     * Dados de um bairro da origem, usados nas ações de vincular e criar.
     *
     * cidadeCorreios / distritoCorreios ficam nulos quando cidade + distrito
     * da origem não estão unificados (ou estão em mais de um distrito).
     */
    public record BairroOrigemDados(
            Long cidade,
            Long distrito,
            Long bairro,
            String nome,
            String uf,
            String nomeCidade,
            String nomeNormalizado,
            Long cidadeCorreios,
            Long distritoCorreios,
            Long qtdDistritosCorreios
    ) {
    }

    /**********************************************************************
     * LISTAGEM
     **********************************************************************/

    public Page<BairroOrigemDTO> filtrarPendentes(
            OrigemBairro origem,
            BairroUnificadoFilter filter,
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

        sqlClassificado.append(
                " order by c.nome_cidade, c.nome, c.cidade_origem, c.distrito_origem, c.bairro_origem"
        );

        String sql = " select p.cidade_origem,"
                + " p.distrito_origem,"
                + " p.bairro_origem,"
                + " p.nome,"
                + " p.uf,"
                + " p.nome_cidade,"
                + " p.cidade_correios,"
                + " p.distrito_correios,"
                + " p.qtd_candidatos,"
                + " p.situacao,"
                + " p.id_sugerido,"
                + " s.nome as nome_sugerido,"
                + " s.bairro_correios as bairro_correios_sugerido,"
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
            query.setParameter("situacao", filter.getSituacao().trim().toUpperCase());
        }

        long inicio = pageable.getOffset();

        query.setParameter("inicio", inicio);
        query.setParameter("fim", inicio + pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = query.getResultList();

        if (linhas == null || linhas.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0L);
        }

        Long total = toLong(linhas.get(0)[13]);

        List<BairroOrigemDTO> lista =
                linhas.stream()
                        .map(linha -> new BairroOrigemDTO(
                                origem.name(),
                                toLong(linha[0]),
                                toLong(linha[1]),
                                toLong(linha[2]),
                                toStr(linha[3]),
                                toStr(linha[4]),
                                toStr(linha[5]),
                                toLong(linha[6]),
                                toLong(linha[7]),
                                toLong(linha[8]),
                                toStr(linha[9]),
                                toLong(linha[10]),
                                toStr(linha[11]),
                                toLong(linha[12])
                        ))
                        .toList();

        return new PageImpl<>(lista, pageable, total == null ? 0L : total);
    }

    /*
     * Todos os bairros pendentes com situação UNICA.
     * Retorna [cidade_origem, distrito_origem, bairro_origem, id_sugerido].
     */
    public List<Long[]> listarUnicos(OrigemBairro origem) {

        BairroUnificadoFilter filter = new BairroUnificadoFilter();

        String sql = " select c.cidade_origem, c.distrito_origem, c.bairro_origem, c.id_sugerido"
                + " from (" + sqlClassificacao(origem, filter) + ") c"
                + " where c.situacao = 'UNICA'"
                + " order by c.cidade_origem, c.distrito_origem, c.bairro_origem";

        Query query = manager.createNativeQuery(sql);

        aplicarParametros(origem, filter, query);

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = query.getResultList();

        List<Long[]> resultado = new ArrayList<>();

        for (Object[] linha : linhas) {
            resultado.add(new Long[]{
                    toLong(linha[0]),
                    toLong(linha[1]),
                    toLong(linha[2]),
                    toLong(linha[3])
            });
        }

        return resultado;
    }

    /**********************************************************************
     * RESUMO
     **********************************************************************/

    public BairroUnificadoResumoDTO buscarResumo(OrigemBairro origem) {

        String filtroValidos =
                " where nvl(o.cidade, 0) > 0 and nvl(o.bairro, 0) > 0";

        Long totalOrigem = count(
                "select count(*) from " + origem.getTabelaBairrosCompleta()
                        + " o" + filtroValidos,
                null
        );

        Long totalVinculados = count(
                "select count(*) from " + origem.getTabelaBairrosCompleta()
                        + " o" + filtroValidos
                        + " and exists (select 1 from " + TABELA_UNIFICADO
                        + " x where " + condicaoChave(origem, "x") + ")",
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

        BairroUnificadoFilter filter = new BairroUnificadoFilter();

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

        long distritoNaoUnificado = porSituacao.getOrDefault("DISTRITO_NAO_UNIFICADO", 0L);
        long unicos = porSituacao.getOrDefault("UNICA", 0L);
        long duplicados = porSituacao.getOrDefault("DUPLICADA", 0L);
        long ambiguos = porSituacao.getOrDefault("AMBIGUA", 0L);
        long semCorrespondencia = porSituacao.getOrDefault("SEM_CORRESPONDENCIA", 0L);

        return new BairroUnificadoResumoDTO(
                origem.name(),
                origem.getDescricao(),
                totalOrigem,
                totalVinculados,
                totalDuplicados,
                totalNaoVincular,
                distritoNaoUnificado + unicos + duplicados + ambiguos + semCorrespondencia,
                distritoNaoUnificado,
                unicos,
                duplicados,
                ambiguos,
                semCorrespondencia
        );
    }

    /**********************************************************************
     * CONSULTAS UNITÁRIAS
     **********************************************************************/

    public BairroOrigemDados buscarBairroOrigem(
            OrigemBairro origem,
            Long cidade,
            Long distrito,
            Long bairro) {

        String sql = " select o.cidade,"
                + " o.distrito,"
                + " o.bairro,"
                + " o.nome,"
                + " " + sqlUfCidadeOrigem(origem) + ","
                + " " + sqlNomeCidadeOrigem(origem) + ","
                + " " + FN_NORMALIZA + "(o.nome),"
                + " dd.cidade_correios,"
                + " dd.distrito_correios,"
                + " nvl(dd.qtd_distrito, 0)"
                + " from " + origem.getTabelaBairrosCompleta() + " o"
                + " left join (" + sqlDeParaDistrito(origem) + ") dd"
                + "   on dd.cid = o.cidade"
                + "  and dd.dist = o.distrito"
                + " where o.cidade = :cidade"
                + "   and o.distrito = :distrito"
                + "   and o.bairro = :bairro"
                + "   and rownum = 1";

        @SuppressWarnings("unchecked")
        List<Object[]> linhas =
                manager.createNativeQuery(sql)
                        .setParameter("origem", origem.name())
                        .setParameter("cidade", cidade)
                        .setParameter("distrito", distrito)
                        .setParameter("bairro", bairro)
                        .getResultList();

        if (linhas == null || linhas.isEmpty()) {
            return null;
        }

        Object[] linha = linhas.get(0);

        Long qtd = toLong(linha[9]);
        boolean unificado = qtd != null && qtd == 1L;

        return new BairroOrigemDados(
                toLong(linha[0]),
                toLong(linha[1]),
                toLong(linha[2]),
                toStr(linha[3]),
                toStr(linha[4]),
                toStr(linha[5]),
                toStr(linha[6]),
                unificado ? toLong(linha[7]) : null,
                unificado ? toLong(linha[8]) : null,
                qtd == null ? 0L : qtd
        );
    }

    public boolean origemJaTratada(
            OrigemBairro origem,
            Long cidade,
            Long distrito,
            Long bairro) {

        Number vinculado =
                (Number) manager.createNativeQuery(
                                "select count(*) from " + TABELA_UNIFICADO
                                        + " x where x." + origem.getColunaCidade() + " = :cidade"
                                        + " and x." + origem.getColunaDistrito() + " = :distrito"
                                        + " and x." + origem.getColunaBairro() + " = :bairro"
                        )
                        .setParameter("cidade", cidade)
                        .setParameter("distrito", distrito)
                        .setParameter("bairro", bairro)
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
                                        + " and k.bairro_origem = :bairro"
                        )
                        .setParameter("origem", origem.name())
                        .setParameter("cidade", cidade)
                        .setParameter("distrito", distrito)
                        .setParameter("bairro", bairro)
                        .getSingleResult();

        return controle != null && controle.longValue() > 0L;
    }

    /*
     * Candidatos:
     *
     * - padrão: mesmo nome normalizado, no mesmo distrito dos Correios;
     * - pesquisa: parte do nome normalizado, em qualquer distrito da cidade;
     * - todos: todos os bairros do distrito dos Correios.
     */
    public List<BairroCandidatoDTO> buscarCandidatos(
            OrigemBairro origem,
            BairroOrigemDados dados,
            String pesquisa,
            boolean todos) {

        if (dados.cidadeCorreios() == null || dados.distritoCorreios() == null) {
            return new ArrayList<>();
        }

        boolean manual = StringUtils.hasText(pesquisa);

        StringBuilder sql = new StringBuilder();

        sql.append("select * from (")
                .append(" select bu.id,")
                .append(" bu.cidade_correios,")
                .append(" bu.distrito_correios,")
                .append(" bu.bairro_correios,")
                .append(" bu.nome,")
                .append(" bu.nome_reduzido,")
                .append(" ").append(sqlCodigoAtual(origem, "bu")).append(",")
                .append(" bu.status_cor,")
                .append(" case")
                .append("   when bu.nome_norm = :nomeNorm")
                .append("    and bu.distrito_correios = :distritoCorreios then 'NOME'")
                .append(manual ? "   else 'PESQUISA'" : "   else 'DISTRITO'")
                .append(" end")
                .append(" from ").append(TABELA_UNIFICADO).append(" bu")
                .append(" where bu.cidade_correios = :cidadeCorreios");

        if (manual) {
            sql.append(" and bu.nome_norm like '%' || ").append(FN_NORMALIZA)
                    .append("(:pesquisa) || '%'");
        } else {
            sql.append(" and bu.distrito_correios = :distritoCorreios");

            if (!todos) {
                sql.append(" and bu.nome_norm = :nomeNorm");
            }
        }

        sql.append(" order by bu.nome, bu.id")
                .append(") where rownum <= ").append(LIMITE_CANDIDATOS);

        Query query =
                manager.createNativeQuery(sql.toString())
                        .setParameter("nomeNorm", nvl(dados.nomeNormalizado()))
                        .setParameter("cidadeCorreios", dados.cidadeCorreios())
                        .setParameter("distritoCorreios", dados.distritoCorreios());

        if (manual) {
            query.setParameter("pesquisa", pesquisa.trim());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = query.getResultList();

        return linhas.stream()
                .map(linha -> new BairroCandidatoDTO(
                        toLong(linha[0]),
                        toLong(linha[1]),
                        toLong(linha[2]),
                        toLong(linha[3]),
                        toStr(linha[4]),
                        toStr(linha[5]),
                        toLong(linha[6]),
                        toStr(linha[7]),
                        toStr(linha[8])
                ))
                .toList();
    }

    /*
     * Cidade dos Correios do bairro unificado. -1 quando o ID não existe.
     */
    public Long buscarCidadeCorreiosDoUnificado(Long idUnificado) {

        @SuppressWarnings("unchecked")
        List<Object> linhas =
                manager.createNativeQuery(
                                "select nvl(bu.cidade_correios, 0) from "
                                        + TABELA_UNIFICADO + " bu where bu.id = :id"
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
     * Grava cidade + distrito + bairro da origem no bairro unificado,
     * somente quando os três ainda estão vazios (null ou 0).
     *
     * Retorna 0 quando o bairro unificado já possui outro código desta origem.
     */
    public int vincularBairro(
            OrigemBairro origem,
            Long idUnificado,
            BairroOrigemDados dados) {

        String sql = "update " + TABELA_UNIFICADO + " bu"
                + " set bu." + origem.getColunaCidade() + " = :cidade,"
                + " bu." + origem.getColunaDistrito() + " = :distrito,"
                + " bu." + origem.getColunaBairro() + " = :bairro,"
                + " bu.data_alteracao = sysdate"
                + " where bu.id = :id"
                + "   and nvl(bu." + origem.getColunaCidade() + ", 0) = 0"
                + "   and nvl(bu." + origem.getColunaDistrito() + ", 0) = 0"
                + "   and nvl(bu." + origem.getColunaBairro() + ", 0) = 0";

        return manager.createNativeQuery(sql)
                .setParameter("cidade", dados.cidade())
                .setParameter("distrito", dados.distrito())
                .setParameter("bairro", dados.bairro())
                .setParameter("id", idUnificado)
                .executeUpdate();
    }

    /*
     * Completa as colunas configuradas em OrigemBairro.getComplementos()
     * com os dados do bairro na origem, somente quando estiverem vazias.
     */
    public void complementarBairro(
            OrigemBairro origem,
            Long idUnificado,
            BairroOrigemDados dados) {

        if (origem.getComplementos().isEmpty()) {
            return;
        }

        StringBuilder sets = new StringBuilder();

        for (OrigemBairro.Complemento complemento : origem.getComplementos()) {

            String coluna = "bu." + complemento.coluna();

            String valorOrigem = "(select " + complemento.expressao()
                    + " from " + origem.getTabelaBairrosCompleta() + " o"
                    + " where o.cidade = :cidade"
                    + " and o.distrito = :distrito"
                    + " and o.bairro = :bairro"
                    + " and rownum = 1)";

            if (sets.length() > 0) {
                sets.append(", ");
            }

            if (complemento.numerico()) {
                sets.append(coluna).append(" = coalesce(nullif(").append(coluna)
                        .append(", 0), ").append(valorOrigem).append(", ").append(coluna).append(")");
            } else {
                sets.append(coluna).append(" = coalesce(").append(coluna)
                        .append(", ").append(valorOrigem).append(")");
            }
        }

        String sql = "update " + TABELA_UNIFICADO + " bu"
                + " set " + sets
                + " where bu.id = :id";

        manager.createNativeQuery(sql)
                .setParameter("cidade", dados.cidade())
                .setParameter("distrito", dados.distrito())
                .setParameter("bairro", dados.bairro())
                .setParameter("id", idUnificado)
                .executeUpdate();
    }

    /*
     * status D: código duplicado da origem, vinculado a idUnificado;
     * status N: não vincular.
     */
    public void registrarControle(
            OrigemBairro origem,
            Long cidadeOrigem,
            Long distritoOrigem,
            Long bairroOrigem,
            String status,
            Long idUnificado,
            String motivo,
            String usuario) {

        manager.createNativeQuery("""
            merge into DBO_UNI_PESSOAS.BAIRROS_UNIFICADO_CTRL destino
            using
            (
                select :origem as origem,
                       :cidadeOrigem as cidade_origem,
                       :distritoOrigem as distrito_origem,
                       :bairroOrigem as bairro_origem
                  from dual
            ) origem
               on
               (
                   destino.ORIGEM = origem.origem
                   and destino.CIDADE_ORIGEM = origem.cidade_origem
                   and destino.DISTRITO_ORIGEM = origem.distrito_origem
                   and destino.BAIRRO_ORIGEM = origem.bairro_origem
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
                    BAIRRO_ORIGEM,
                    STATUS,
                    ID_UNIFICADO,
                    MOTIVO,
                    USUARIO,
                    DATA_REGISTRO
                )
                values
                (
                    DBO_UNI_PESSOAS.SEQ_BAIRROS_UNIFICADO_CTRL.nextval,
                    :origem,
                    :cidadeOrigem,
                    :distritoOrigem,
                    :bairroOrigem,
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
                .setParameter("bairroOrigem", bairroOrigem)
                .setParameter("status", status)
                .setParameter("idUnificado", idUnificado)
                .setParameter("motivo", motivo)
                .setParameter("usuario", usuario)
                .executeUpdate();
    }

    /*
     * Cria o bairro que não existe na base dos Correios:
     *
     * 1. DBO_CORREIOS.BAIRROS no distrito dos Correios, STATUS = 'N',
     *    código pela sequence;
     * 2. DBO_UNI_PESSOAS.BAIRROS_UNIFICADO com o código desta origem
     *    e zero nas demais origens.
     *
     * Os complementos são gravados depois, por complementarBairro.
     * Retorna o ID gerado em BAIRROS_UNIFICADO.
     */
    public Long criarBairro(
            OrigemBairro origem,
            BairroOrigemDados dados) {

        Long bairroCorreios = nextVal(SEQ_CORREIOS);

        manager.createNativeQuery(
                        "insert into " + TABELA_CORREIOS
                                + " (CIDADE, DISTRITO, BAIRRO, NOME, NOME_REDUZIDO, NOME_TRANSF, STATUS)"
                                + " values (:cidade, :distrito, :bairro,"
                                + " upper(trim(:nome)),"
                                + " substr(upper(trim(:nome)), 1, 30),"
                                + " " + FN_NORMALIZA + "(:nome),"
                                + " 'N')"
                )
                .setParameter("cidade", dados.cidadeCorreios())
                .setParameter("distrito", dados.distritoCorreios())
                .setParameter("bairro", bairroCorreios)
                .setParameter("nome", nvl(dados.nome()))
                .executeUpdate();

        Long idUnificado = nextVal(SEQ_UNIFICADO);

        StringBuilder colunasOrigem = new StringBuilder();
        StringBuilder valoresOrigem = new StringBuilder();

        for (OrigemBairro item : OrigemBairro.values()) {

            colunasOrigem.append(item.getColunaCidade()).append(", ")
                    .append(item.getColunaDistrito()).append(", ")
                    .append(item.getColunaBairro()).append(", ");

            if (item == origem) {
                valoresOrigem.append(":cidadeOrigem, :distritoOrigem, :bairroOrigem, ");
            } else {
                valoresOrigem.append("0, 0, 0, ");
            }
        }

        String sql = "insert into " + TABELA_UNIFICADO
                + " (ID, CIDADE_CORREIOS, DISTRITO_CORREIOS, BAIRRO_CORREIOS, "
                + colunasOrigem
                + "NOME, NOME_CORREIOS, NOME_REDUZIDO, NOME_NORM,"
                + " DATA_ALTERACAO, USUARIO_ALTERACAO, STATUS_COR)"
                + " values (:id, :cidadeCorreios, :distritoCorreios, :bairroCorreios, "
                + valoresOrigem
                + "upper(trim(:nome)), upper(trim(:nome)), substr(upper(trim(:nome)), 1, 30),"
                + " " + FN_NORMALIZA + "(:nome),"
                + " sysdate, null, :statusCor)";

        manager.createNativeQuery(sql)
                .setParameter("id", idUnificado)
                .setParameter("cidadeCorreios", dados.cidadeCorreios())
                .setParameter("distritoCorreios", dados.distritoCorreios())
                .setParameter("bairroCorreios", bairroCorreios)
                .setParameter("cidadeOrigem", dados.cidade())
                .setParameter("distritoOrigem", dados.distrito())
                .setParameter("bairroOrigem", dados.bairro())
                .setParameter("nome", nvl(dados.nome()))
                .setParameter("statusCor", origem.getStatusCor())
                .executeUpdate();

        return idUnificado;
    }


    /**********************************************************************
     * LOTE EM CONJUNTO (vincular únicos)
     *
     * O lote antigo tratava um registro por vez (5 a 6 consultas cada);
     * com dezenas de milhares de pendentes a requisição passava de horas
     * e o navegador desistia ("0 Unknown Error"). Agora são três comandos:
     *
     * 1. registrarDuplicadosDoLote: quando dois ou mais pendentes do lote
     *    apontam para o mesmo unificado, o primeiro (menor código) é
     *    vinculado e os demais vão para o controle como duplicados (D),
     *    exatamente o que o processamento um a um fazia;
     * 2. vincularLote: grava o código da origem nos unificados ainda vazios;
     * 3. complementarVinculados: preenche as colunas vazias dos unificados
     *    vinculados a esta origem.
     **********************************************************************/

    private String sqlLote(OrigemBairro origem) {

        BairroUnificadoFilter filter = new BairroUnificadoFilter();

        return " select c.cidade_origem, c.distrito_origem, c.bairro_origem, c.id_sugerido,"
                + " row_number() over (partition by c.id_sugerido"
                + "   order by c.cidade_origem, c.distrito_origem, c.bairro_origem) as rn"
                + " from (" + sqlClassificacao(origem, filter) + ") c"
                + " where c.situacao in ('UNICA')"
                + "   and c.id_sugerido is not null";
    }

    public long registrarDuplicadosDoLote(OrigemBairro origem) {

        String sql = "insert into " + TABELA_CTRL
                + " (ID, ORIGEM, CIDADE_ORIGEM, DISTRITO_ORIGEM, BAIRRO_ORIGEM,"
                + " STATUS, ID_UNIFICADO, MOTIVO, USUARIO, DATA_REGISTRO)"
                + " select " + SCHEMA + ".SEQ_BAIRROS_UNIFICADO_CTRL.nextval,"
                + " :origem, l.cidade_origem, l.distrito_origem, l.bairro_origem,"
                + " '" + STATUS_DUPLICADO + "', l.id_sugerido,"
                + " 'Código duplicado na origem (lote).', null, sysdate"
                + " from (" + sqlLote(origem) + ") l"
                + " where l.rn > 1";

        return manager.createNativeQuery(sql)
                .setParameter("origem", origem.name())
                .executeUpdate();
    }

    public long vincularLote(OrigemBairro origem) {

        String cid = "u." + origem.getColunaCidade();
        String dist = "u." + origem.getColunaDistrito();
        String cod = "u." + origem.getColunaBairro();

        String sql = "merge into " + TABELA_UNIFICADO + " u"
                + " using (select * from (" + sqlLote(origem) + ") where rn = 1) s"
                + " on (u.id = s.id_sugerido)"
                + " when matched then update set"
                + " " + cid + " = s.cidade_origem,"
                + " " + dist + " = s.distrito_origem,"
                + " " + cod + " = s.bairro_origem,"
                + " u.data_alteracao = sysdate"
                + " where nvl(" + cid + ", 0) = 0"
                + "   and nvl(" + dist + ", 0) = 0"
                + "   and nvl(" + cod + ", 0) = 0";

        return manager.createNativeQuery(sql)
                .setParameter("origem", origem.name())
                .executeUpdate();
    }

    public long complementarVinculados(OrigemBairro origem) {

        if (origem.getComplementos().isEmpty()) {
            return 0L;
        }

        String cid = "u." + origem.getColunaCidade();
        String dist = "u." + origem.getColunaDistrito();
        String cod = "u." + origem.getColunaBairro();

        StringBuilder sets = new StringBuilder();
        StringBuilder vazios = new StringBuilder();

        for (OrigemBairro.Complemento complemento : origem.getComplementos()) {

            String coluna = "u." + complemento.coluna();

            String valorOrigem = "(select " + complemento.expressao()
                    + " from " + origem.getTabelaBairrosCompleta() + " o"
                    + " where o.cidade = " + cid
                    + " and o.distrito = " + dist
                    + " and o.bairro = " + cod
                    + " and rownum = 1)";

            if (sets.length() > 0) {
                sets.append(", ");
                vazios.append(" or ");
            }

            if (complemento.numerico()) {
                sets.append(coluna).append(" = coalesce(nullif(").append(coluna)
                        .append(", 0), ").append(valorOrigem).append(", ").append(coluna).append(")");
                vazios.append("nvl(").append(coluna).append(", 0) = 0");
            } else {
                sets.append(coluna).append(" = coalesce(").append(coluna)
                        .append(", ").append(valorOrigem).append(")");
                vazios.append(coluna).append(" is null");
            }
        }

        String sql = "update " + TABELA_UNIFICADO + " u"
                + " set " + sets
                + " where nvl(" + cid + ", 0) > 0"
                + "   and nvl(" + cod + ", 0) > 0"
                + "   and (" + vazios + ")";

        return manager.createNativeQuery(sql).executeUpdate();
    }

    /**********************************************************************
     * SQL BASE
     **********************************************************************/

    /*
     * De-para: cidade + distrito da origem -> cidade + distrito dos Correios.
     *
     * Considera o par gravado em DISTRITOS_UNIFICADO e os pares duplicados
     * registrados em DISTRITOS_UNIFICADO_CTRL (status D).
     */
    private String sqlDeParaDistrito(OrigemBairro origem) {

        OrigemDistrito od = origem.getOrigemDistrito();

        return " select x.cid,"
                + " x.dist,"
                + " count(distinct du.cidade_correios || '-' || du.distrito_correios) as qtd_distrito,"
                + " min(du.cidade_correios) as cidade_correios,"
                + " min(du.distrito_correios) as distrito_correios"
                + " from ("
                + "   select du1." + od.getColunaCidade() + " as cid,"
                + "          du1." + od.getColunaDistrito() + " as dist,"
                + "          du1.id"
                + "     from " + TABELA_DISTRITOS_UNIFICADO + " du1"
                + "    where nvl(du1." + od.getColunaCidade() + ", 0) > 0"
                + "   union"
                + "   select k.cidade_origem, k.distrito_origem, k.id_unificado"
                + "     from " + TABELA_DISTRITOS_CTRL + " k"
                + "    where k.origem = :origem"
                + "      and k.status = 'D'"
                + "      and k.id_unificado is not null"
                + " ) x"
                + " join " + TABELA_DISTRITOS_UNIFICADO + " du"
                + "   on du.id = x.id"
                + " where nvl(du.cidade_correios, 0) > 0"
                + " group by x.cid, x.dist";
    }

    private String sqlUfCidadeOrigem(OrigemBairro origem) {
        return "(select max(upper(trim(c.estado))) from "
                + origem.getTabelaCidadesCompleta()
                + " c where c.cidade = o.cidade)";
    }

    private String sqlNomeCidadeOrigem(OrigemBairro origem) {
        return "(select max(c.nome) from "
                + origem.getTabelaCidadesCompleta()
                + " c where c.cidade = o.cidade)";
    }

    private String condicaoChave(OrigemBairro origem, String alias) {
        return alias + "." + origem.getColunaCidade() + " = o.cidade"
                + " and " + alias + "." + origem.getColunaDistrito() + " = o.distrito"
                + " and " + alias + "." + origem.getColunaBairro() + " = o.bairro";
    }

    private String sqlCodigoAtual(OrigemBairro origem, String alias) {
        return "greatest(nvl(" + alias + "." + origem.getColunaCidade() + ", 0),"
                + " nvl(" + alias + "." + origem.getColunaDistrito() + ", 0),"
                + " nvl(" + alias + "." + origem.getColunaBairro() + ", 0))";
    }

    /*
     * Bairros da origem ainda não tratados.
     */
    private String sqlOrigemPendente(
            OrigemBairro origem,
            BairroUnificadoFilter filter) {

        StringBuilder sql = new StringBuilder();

        sql.append(" select t.* from (")
                .append(" select o.cidade as cidade_origem,")
                .append(" o.distrito as distrito_origem,")
                .append(" o.bairro as bairro_origem,")
                .append(" o.nome as nome,")
                .append(" ").append(sqlUfCidadeOrigem(origem)).append(" as uf,")
                .append(" ").append(sqlNomeCidadeOrigem(origem)).append(" as nome_cidade,")
                .append(" ").append(FN_NORMALIZA).append("(o.nome) as nome_norm,")
                .append(" case when dd.qtd_distrito = 1 then dd.cidade_correios end as cidade_correios,")
                .append(" case when dd.qtd_distrito = 1 then dd.distrito_correios end as distrito_correios,")
                .append(" nvl(dd.qtd_distrito, 0) as qtd_distrito")
                .append(" from ").append(origem.getTabelaBairrosCompleta()).append(" o")
                .append(" left join (").append(sqlDeParaDistrito(origem)).append(") dd")
                .append("   on dd.cid = o.cidade")
                .append("  and dd.dist = o.distrito")
                .append(" where nvl(o.cidade, 0) > 0")
                .append("   and nvl(o.bairro, 0) > 0")
                .append("   and not exists (select 1 from ").append(TABELA_UNIFICADO)
                .append("        x where ").append(condicaoChave(origem, "x")).append(")")
                .append("   and not exists (select 1 from ").append(TABELA_CTRL)
                .append("        k where k.origem = :origem")
                .append("        and k.cidade_origem = o.cidade")
                .append("        and k.distrito_origem = o.distrito")
                .append("        and k.bairro_origem = o.bairro)");

        if (filter.getCidade() != null) {
            sql.append(" and o.cidade = :cidade");
        }

        if (filter.getDistrito() != null) {
            sql.append(" and o.distrito = :distrito");
        }

        if (filter.getBairro() != null) {
            sql.append(" and o.bairro = :bairro");
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

    private String sqlUnificadoNormalizado(OrigemBairro origem) {

        return " select bu.id,"
                + " bu.cidade_correios,"
                + " bu.distrito_correios,"
                + " bu.nome_norm,"
                + " " + sqlCodigoAtual(origem, "bu") + " as cod_atual"
                + " from " + TABELA_UNIFICADO + " bu"
                + " where nvl(bu.cidade_correios, 0) > 0"
                + "   and bu.nome_norm is not null";
    }

    /*
     * Classificação em uma única passada pela origem: cada pendente é juntado
     * aos bairros unificados de mesmo nome normalizado (coluna NOME_NORM,
     * indexada) no mesmo distrito dos Correios, e agrupado.
     *
     * A versão anterior calculava FN_NORMALIZA_CIDADE em todos os bairros
     * unificados e lia a origem duas vezes; com ~90 mil bairros a listagem
     * não terminava.
     */
    private String sqlClassificacao(
            OrigemBairro origem,
            BairroUnificadoFilter filter) {

        String pendentes = sqlOrigemPendente(origem, filter);
        String unificado = sqlUnificadoNormalizado(origem);

        String agrupado = " select po.cidade_origem,"
                + " po.distrito_origem,"
                + " po.bairro_origem,"
                + " po.nome,"
                + " po.uf,"
                + " po.nome_cidade,"
                + " po.cidade_correios,"
                + " po.distrito_correios,"
                + " po.qtd_distrito,"
                + " count(un.id) as qtd,"
                + " min(un.id) as id_min,"
                + " sum(case when un.cod_atual > 0 then 1 else 0 end) as qtd_ocupado"
                + " from (" + pendentes + ") po"
                + " left join (" + unificado + ") un"
                + "   on po.qtd_distrito = 1"
                + "  and un.cidade_correios = po.cidade_correios"
                + "  and un.distrito_correios = po.distrito_correios"
                + "  and un.nome_norm = po.nome_norm"
                + " group by po.cidade_origem, po.distrito_origem, po.bairro_origem,"
                + " po.nome, po.uf, po.nome_cidade,"
                + " po.cidade_correios, po.distrito_correios, po.qtd_distrito";

        return " select g.cidade_origem,"
                + " g.distrito_origem,"
                + " g.bairro_origem,"
                + " g.nome,"
                + " g.uf,"
                + " g.nome_cidade,"
                + " g.cidade_correios,"
                + " g.distrito_correios,"
                + " g.qtd as qtd_candidatos,"
                + " case when g.qtd = 1 then g.id_min end as id_sugerido,"
                + " case"
                + "   when g.qtd_distrito <> 1 then 'DISTRITO_NAO_UNIFICADO'"
                + "   when g.qtd = 0 then 'SEM_CORRESPONDENCIA'"
                + "   when g.qtd > 1 then 'AMBIGUA'"
                + "   when g.qtd_ocupado > 0 then 'DUPLICADA'"
                + "   else 'UNICA'"
                + " end as situacao"
                + " from (" + agrupado + ") g";
    }

    private void aplicarParametros(
            OrigemBairro origem,
            BairroUnificadoFilter filter,
            Query query) {

        query.setParameter("origem", origem.name());

        if (filter.getCidade() != null) {
            query.setParameter("cidade", filter.getCidade());
        }

        if (filter.getDistrito() != null) {
            query.setParameter("distrito", filter.getDistrito());
        }

        if (filter.getBairro() != null) {
            query.setParameter("bairro", filter.getBairro());
        }

        if (StringUtils.hasText(filter.getNome())) {
            query.setParameter("nome", "%" + filter.getNome().trim().toUpperCase() + "%");
        }

        if (StringUtils.hasText(filter.getNomeCidade())) {
            query.setParameter("nomeCidade", "%" + filter.getNomeCidade().trim().toUpperCase() + "%");
        }

        if (StringUtils.hasText(filter.getUf())) {
            query.setParameter("uf", filter.getUf().trim().toUpperCase());
        }
    }

    /**********************************************************************
     * UTILITÁRIOS
     **********************************************************************/

    private Long count(String sql, OrigemBairro origem) {

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

    private static String toStr(Object valor) {
        return valor == null ? null : valor.toString().trim();
    }

    private static String nvl(String valor) {
        return valor == null ? "" : valor;
    }
}
