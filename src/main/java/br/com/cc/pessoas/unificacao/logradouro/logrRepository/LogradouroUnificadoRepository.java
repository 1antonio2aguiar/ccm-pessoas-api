package br.com.cc.pessoas.unificacao.logradouro.logrRepository;

import br.com.cc.pessoas.unificacao.cidade.cidEnum.OrigemCidade;
import br.com.cc.pessoas.unificacao.distrito.distEnum.OrigemDistrito;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroCandidatoDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroCepDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroOrigemDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrEnum.OrigemLogradouro;
import br.com.cc.pessoas.unificacao.logradouro.logrFilter.LogradouroUnificadoFilter;
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
 * Consultas e gravações da unificação de logradouros.
 *
 * Tabelas envolvidas:
 *
 * - DBO_UNI_PESSOAS.<ORIGEM>_LOGRADOUROS / _TIPOS_LOGRADOUROS / _CIDADES (sinônimos, leitura)
 * - DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO e DISTRITOS_UNIFICADO_CTRL (leitura:
 *   de-para de cidade + distrito da origem para cidade + distrito dos Correios)
 * - DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO      (de-para por sistema)
 * - DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO_CTRL (duplicados e "não vincular")
 * - DBO_CORREIOS.LOGRADOUROS e DBO_CORREIOS.CEPS (somente inclusão de logradouro
 *   novo, STATUS = 'N', com um CEP que JÁ EXISTE em CEPS_CARGA_DNE)
 *
 * Comparação: chave do nome (PKG_LOGRADOURO_CHAVE.CHAVE) dentro do mesmo
 * distrito dos Correios. Do lado dos Correios a chave fica gravada em
 * LOGRADOUROS_UNIFICADO.CHAVE_NOME (indexada); do lado da origem é calculada
 * na consulta. O tipo é comparado à parte (CHAVE_TIPO).
 *
 * SQL compatível com Oracle 11g: sem WITH, sem FETCH FIRST, paginação por ROWNUM.
 */
@Repository
public class LogradouroUnificadoRepository {

    private static final String SCHEMA =
            OrigemCidade.SCHEMA_UNIFICADO;

    private static final String TABELA_DISTRITOS_UNIFICADO =
            SCHEMA + ".DISTRITOS_UNIFICADO";

    private static final String TABELA_DISTRITOS_CTRL =
            SCHEMA + ".DISTRITOS_UNIFICADO_CTRL";

    private static final String TABELA_UNIFICADO =
            SCHEMA + ".LOGRADOUROS_UNIFICADO";

    private static final String TABELA_CTRL =
            SCHEMA + ".LOGRADOUROS_UNIFICADO_CTRL";

    private static final String PKG_CHAVE =
            SCHEMA + ".PKG_LOGRADOURO_CHAVE";

    private static final String TABELA_CORREIOS =
            "DBO_CORREIOS.LOGRADOUROS";

    private static final String TABELA_CEPS_CORREIOS =
            "DBO_CORREIOS.CEPS";

    private static final String TABELA_CEPS_DNE =
            "DBO_CORREIOS.CEPS_CARGA_DNE";

    private static final String TABELA_BAIRROS_CORREIOS =
            "DBO_CORREIOS.BAIRROS";

    private static final String FN_TRANSF =
            "DBO_CORREIOS.TRANSF_CARACT";

    private static final String SEQ_CORREIOS =
            "DBO_CORREIOS.SEQ_LOGRADOUROS_FICTICIOS";

    private static final String SEQ_UNIFICADO =
            SCHEMA + ".SEQ_LOGRADOUROS_UNIFICADO";

    public static final String STATUS_DUPLICADO = "D";
    public static final String STATUS_NAO_VINCULAR = "N";

    private static final int LIMITE_CANDIDATOS = 500;
    private static final int LIMITE_CEPS = 200;

    /*
     * Complemento dos Correios que indica faixa de numeração ou lado:
     * "- até 699/700", "- de 701/702 ao fim", "- lado ímpar".
     */
    private static final String SQL_FAIXA =
            "case when regexp_like(%1$s.complemento, '[0-9]')"
                    + " or upper(%1$s.complemento) like '%%LADO%%'"
                    + " or upper(%1$s.complemento) like '%%FIM%%'"
                    + " then 1 else 0 end";

    @PersistenceContext
    private EntityManager manager;

    /*
     * Dados de um logradouro da origem, usados nas ações de vincular e criar.
     *
     * cidadeCorreios / distritoCorreios ficam nulos quando cidade + distrito
     * da origem não estão unificados (ou estão em mais de um distrito).
     */
    public record LogradouroOrigemDados(
            Long cidade,
            Long distrito,
            Long logradouro,
            String nome,
            String tipo,
            String uf,
            String nomeCidade,
            String chaveNome,
            String chaveTipo,
            Long cidadeCorreios,
            Long distritoCorreios,
            Long qtdDistritosCorreios,
            Long cepOrigem
    ) {
    }

    /**********************************************************************
     * LISTAGEM
     **********************************************************************/

    public Page<LogradouroOrigemDTO> filtrarPendentes(
            OrigemLogradouro origem,
            LogradouroUnificadoFilter filter,
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
                " order by c.nome_cidade, c.nome, c.cidade_origem, c.distrito_origem, c.logradouro_origem"
        );

        String sql = " select p.cidade_origem,"
                + " p.distrito_origem,"
                + " p.logradouro_origem,"
                + " p.tipo,"
                + " p.nome,"
                + " p.uf,"
                + " p.nome_cidade,"
                + " p.cidade_correios,"
                + " p.distrito_correios,"
                + " p.qtd_candidatos,"
                + " p.situacao,"
                + " p.id_sugerido,"
                + " trim(s.tipo_logradouro || ' ' || nvl(s.nome_correios, s.nome)) as nome_sugerido,"
                + " s.logradouro_correios as logradouro_correios_sugerido,"
                + " s.complemento as complemento_sugerido,"
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

        Long total = toLong(linhas.get(0)[15]);

        List<LogradouroOrigemDTO> lista =
                linhas.stream()
                        .map(linha -> new LogradouroOrigemDTO(
                                origem.name(),
                                toLong(linha[0]),
                                toLong(linha[1]),
                                toLong(linha[2]),
                                toStr(linha[3]),
                                toStr(linha[4]),
                                toStr(linha[5]),
                                toStr(linha[6]),
                                toLong(linha[7]),
                                toLong(linha[8]),
                                toLong(linha[9]),
                                toStr(linha[10]),
                                toLong(linha[11]),
                                toStr(linha[12]),
                                toLong(linha[13]),
                                toStr(linha[14])
                        ))
                        .toList();

        return new PageImpl<>(lista, pageable, total == null ? 0L : total);
    }

    /*
     * Logradouros pendentes que entram no lote: UNICA e FAIXAS.
     * Retorna [cidade_origem, distrito_origem, logradouro_origem, id_sugerido].
     */
    public List<Long[]> listarParaLote(OrigemLogradouro origem) {

        LogradouroUnificadoFilter filter = new LogradouroUnificadoFilter();

        String sql = " select c.cidade_origem, c.distrito_origem, c.logradouro_origem, c.id_sugerido"
                + " from (" + sqlClassificacao(origem, filter) + ") c"
                + " where c.situacao in ('UNICA', 'FAIXAS')"
                + " order by c.cidade_origem, c.distrito_origem, c.logradouro_origem";

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

    public LogradouroUnificadoResumoDTO buscarResumo(OrigemLogradouro origem) {

        String filtroValidos =
                " where nvl(o.cidade, 0) > 0 and nvl(o.logradouro, 0) > 0";

        Long totalOrigem = count(
                "select count(*) from " + origem.getTabelaLogradourosCompleta()
                        + " o" + filtroValidos,
                null
        );

        Long totalVinculados = count(
                "select count(*) from " + origem.getTabelaLogradourosCompleta()
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

        LogradouroUnificadoFilter filter = new LogradouroUnificadoFilter();

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
        long faixas = porSituacao.getOrDefault("FAIXAS", 0L);
        long duplicados = porSituacao.getOrDefault("DUPLICADA", 0L);
        long tipoDiferente = porSituacao.getOrDefault("TIPO_DIFERENTE", 0L);
        long ambiguos = porSituacao.getOrDefault("AMBIGUA", 0L);
        long semCorrespondencia = porSituacao.getOrDefault("SEM_CORRESPONDENCIA", 0L);

        return new LogradouroUnificadoResumoDTO(
                origem.name(),
                origem.getDescricao(),
                totalOrigem,
                totalVinculados,
                totalDuplicados,
                totalNaoVincular,
                distritoNaoUnificado + unicos + faixas + duplicados
                        + tipoDiferente + ambiguos + semCorrespondencia,
                distritoNaoUnificado,
                unicos,
                faixas,
                duplicados,
                tipoDiferente,
                ambiguos,
                semCorrespondencia
        );
    }

    /**********************************************************************
     * CONSULTAS UNITÁRIAS
     **********************************************************************/

    public LogradouroOrigemDados buscarLogradouroOrigem(
            OrigemLogradouro origem,
            Long cidade,
            Long distrito,
            Long logradouro) {

        boolean temCep = origem == OrigemLogradouro.SAU;

        String sql = " select t.cidade, t.distrito, t.logradouro, t.nome, t.tipo, t.uf, t.nome_cidade,"
                + " " + PKG_CHAVE + ".chave(null, null, t.nome),"
                + " " + PKG_CHAVE + ".tipo(t.tipo),"
                + " t.cidade_correios, t.distrito_correios, t.qtd_distrito, t.cep"
                + " from ("
                + " select o.cidade,"
                + " o.distrito,"
                + " o.logradouro,"
                + " o.nome,"
                + " " + sqlTipoOrigem(origem) + " as tipo,"
                + " " + sqlUfCidadeOrigem(origem) + " as uf,"
                + " " + sqlNomeCidadeOrigem(origem) + " as nome_cidade,"
                + " dd.cidade_correios,"
                + " dd.distrito_correios,"
                + " nvl(dd.qtd_distrito, 0) as qtd_distrito,"
                + " " + (temCep ? "o.cep" : "null") + " as cep"
                + " from " + origem.getTabelaLogradourosCompleta() + " o"
                + " left join (" + sqlDeParaDistrito(origem) + ") dd"
                + "   on dd.cid = o.cidade"
                + "  and dd.dist = o.distrito"
                + " where o.cidade = :cidade"
                + "   and o.distrito = :distrito"
                + "   and o.logradouro = :logradouro"
                + "   and rownum = 1"
                + " ) t";

        @SuppressWarnings("unchecked")
        List<Object[]> linhas =
                manager.createNativeQuery(sql)
                        .setParameter("origem", origem.name())
                        .setParameter("cidade", cidade)
                        .setParameter("distrito", distrito)
                        .setParameter("logradouro", logradouro)
                        .getResultList();

        if (linhas == null || linhas.isEmpty()) {
            return null;
        }

        Object[] linha = linhas.get(0);

        Long qtd = toLong(linha[11]);
        boolean unificado = qtd != null && qtd == 1L;

        return new LogradouroOrigemDados(
                toLong(linha[0]),
                toLong(linha[1]),
                toLong(linha[2]),
                toStr(linha[3]),
                toStr(linha[4]),
                toStr(linha[5]),
                toStr(linha[6]),
                toStr(linha[7]),
                toStr(linha[8]),
                unificado ? toLong(linha[9]) : null,
                unificado ? toLong(linha[10]) : null,
                qtd == null ? 0L : qtd,
                toLong(linha[12])
        );
    }

    public boolean origemJaTratada(
            OrigemLogradouro origem,
            Long cidade,
            Long distrito,
            Long logradouro) {

        Number vinculado =
                (Number) manager.createNativeQuery(
                                "select count(*) from " + TABELA_UNIFICADO
                                        + " x where x." + origem.getColunaCidade() + " = :cidade"
                                        + " and x." + origem.getColunaDistrito() + " = :distrito"
                                        + " and x." + origem.getColunaLogradouro() + " = :logradouro"
                        )
                        .setParameter("cidade", cidade)
                        .setParameter("distrito", distrito)
                        .setParameter("logradouro", logradouro)
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
                                        + " and k.logradouro_origem = :logradouro"
                        )
                        .setParameter("origem", origem.name())
                        .setParameter("cidade", cidade)
                        .setParameter("distrito", distrito)
                        .setParameter("logradouro", logradouro)
                        .getSingleResult();

        return controle != null && controle.longValue() > 0L;
    }

    /*
     * Candidatos:
     *
     * - padrão: mesma chave de nome, no mesmo distrito dos Correios
     *   (inclui os de tipo diferente, marcados como TIPO_DIFERENTE);
     * - pesquisa: parte da chave do nome, em qualquer distrito da cidade;
     * - todos: todos os logradouros do distrito dos Correios.
     */
    public List<LogradouroCandidatoDTO> buscarCandidatos(
            OrigemLogradouro origem,
            LogradouroOrigemDados dados,
            String pesquisa,
            boolean todos) {

        if (dados.cidadeCorreios() == null || dados.distritoCorreios() == null) {
            return new ArrayList<>();
        }

        boolean manual = StringUtils.hasText(pesquisa);

        StringBuilder sql = new StringBuilder();

        sql.append("select * from (")
                .append(" select lu.id,")
                .append(" lu.cidade_correios,")
                .append(" lu.distrito_correios,")
                .append(" lu.logradouro_correios,")
                .append(" lu.tipo_logradouro,")
                .append(" nvl(lu.nome_correios, lu.nome),")
                .append(" lu.complemento,")
                .append(" (select max(b.nome) from ").append(TABELA_BAIRROS_CORREIOS)
                .append("   b where b.cidade = lu.cidade_correios and b.bairro = lu.bairro),")
                .append(" ").append(String.format(SQL_FAIXA, "lu")).append(",")
                .append(" ").append(sqlCodigoAtual(origem, "lu")).append(",")
                .append(" lu.status_cor,")
                .append(" case")
                .append("   when lu.chave_nome = :chaveNome")
                .append("    and lu.distrito_correios = :distritoCorreios")
                .append("    and ").append(sqlTipoCompativel("lu.chave_tipo", ":chaveTipo"))
                .append("   then 'NOME'")
                .append("   when lu.chave_nome = :chaveNome")
                .append("    and lu.distrito_correios = :distritoCorreios then 'TIPO_DIFERENTE'")
                .append(manual ? "   else 'PESQUISA'" : "   else 'DISTRITO'")
                .append(" end")
                .append(" from ").append(TABELA_UNIFICADO).append(" lu")
                .append(" where lu.cidade_correios = :cidadeCorreios");

        if (manual) {
            sql.append(" and lu.chave_nome like '%' || ")
                    .append(PKG_CHAVE).append(".chave(null, null, :pesquisa) || '%'");
        } else {
            sql.append(" and lu.distrito_correios = :distritoCorreios");

            if (!todos) {
                sql.append(" and lu.chave_nome = :chaveNome");
            }
        }

        sql.append(" order by lu.nome, lu.logradouro_correios, lu.id")
                .append(") where rownum <= ").append(LIMITE_CANDIDATOS);

        Query query =
                manager.createNativeQuery(sql.toString())
                        .setParameter("chaveNome", nvl(dados.chaveNome()))
                        .setParameter("chaveTipo", nvl(dados.chaveTipo()))
                        .setParameter("cidadeCorreios", dados.cidadeCorreios())
                        .setParameter("distritoCorreios", dados.distritoCorreios());

        if (manual) {
            query.setParameter("pesquisa", pesquisa.trim());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = query.getResultList();

        return linhas.stream()
                .map(linha -> new LogradouroCandidatoDTO(
                        toLong(linha[0]),
                        toLong(linha[1]),
                        toLong(linha[2]),
                        toLong(linha[3]),
                        toStr(linha[4]),
                        toStr(linha[5]),
                        toStr(linha[6]),
                        toStr(linha[7]),
                        Long.valueOf(1L).equals(toLong(linha[8])),
                        toLong(linha[9]),
                        toStr(linha[10]),
                        toStr(linha[11])
                ))
                .toList();
    }

    /*
     * CEPs válidos da cidade dos Correios (CEPS_CARGA_DNE), para o logradouro
     * que será criado. Pesquisa: só números = começo do CEP; texto = parte do
     * nome do logradouro dono do CEP. Sem pesquisa: o CEP informado na origem
     * (Saúde), se for válido.
     */
    public List<LogradouroCepDTO> buscarCeps(
            LogradouroOrigemDados dados,
            String pesquisa) {

        if (dados.cidadeCorreios() == null) {
            return new ArrayList<>();
        }

        String texto = pesquisa == null ? "" : pesquisa.trim();
        String digitos = texto.replaceAll("[^0-9]", "");
        boolean porCep = !texto.isEmpty() && texto.replaceAll("[0-9.\\-\\s]", "").isEmpty();
        boolean porNome = !texto.isEmpty() && !porCep;

        if (texto.isEmpty() && dados.cepOrigem() == null) {
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder();

        sql.append("select * from (")
                .append(" select c.cep,")
                .append(" c.distrito,")
                .append(" c.logradouro,")
                .append(" (select max(trim(l.tipo_logradouro || ' '")
                .append("    || nvl(l.nome_completo, l.nome))) from ").append(TABELA_CORREIOS)
                .append("    l where l.cidade = c.cidade and l.distrito = c.distrito")
                .append("    and l.logradouro = c.logradouro),")
                .append(" c.bairro,")
                .append(" (select max(b.nome) from ").append(TABELA_BAIRROS_CORREIOS)
                .append("    b where b.cidade = c.cidade and b.bairro = c.bairro),")
                .append(" c.numero_ini,")
                .append(" c.numero_fin,")
                .append(" c.complemento")
                .append(" from ").append(TABELA_CEPS_DNE).append(" c")
                .append(" where c.cidade = :cidadeCorreios")
                .append("   and c.cep is not null");

        if (porCep) {
            sql.append(" and to_char(c.cep) like :cepInicio");
        } else if (porNome) {
            sql.append(" and exists (select 1 from ").append(TABELA_UNIFICADO)
                    .append("   lu where lu.cidade_correios = c.cidade")
                    .append("   and lu.distrito_correios = c.distrito")
                    .append("   and lu.logradouro_correios = c.logradouro")
                    .append("   and lu.chave_nome like '%' || ")
                    .append(PKG_CHAVE).append(".chave(null, null, :pesquisa) || '%')");
        } else {
            sql.append(" and c.cep = :cepOrigem");
        }

        sql.append(" order by case when c.distrito = :distritoCorreios then 0 else 1 end,")
                .append(" c.cep, c.logradouro")
                .append(") where rownum <= ").append(LIMITE_CEPS);

        Query query =
                manager.createNativeQuery(sql.toString())
                        .setParameter("cidadeCorreios", dados.cidadeCorreios())
                        .setParameter("distritoCorreios", nvl(dados.distritoCorreios()));

        if (porCep) {
            query.setParameter("cepInicio", digitos + "%");
        } else if (porNome) {
            query.setParameter("pesquisa", texto);
        } else {
            query.setParameter("cepOrigem", dados.cepOrigem());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = query.getResultList();

        return linhas.stream()
                .map(linha -> new LogradouroCepDTO(
                        toLong(linha[0]),
                        toLong(linha[1]),
                        toLong(linha[2]),
                        toStr(linha[3]),
                        toLong(linha[4]),
                        toStr(linha[5]),
                        toLong(linha[6]),
                        toLong(linha[7]),
                        toStr(linha[8])
                ))
                .toList();
    }

    /*
     * Bairro dos Correios do CEP na cidade, ou -1 quando o CEP não existe
     * na tabela de CEPs dos Correios para esta cidade.
     */
    public Long buscarBairroDoCepValido(Long cidadeCorreios, Long cep) {

        @SuppressWarnings("unchecked")
        List<Object[]> linhas =
                manager.createNativeQuery(
                                "select count(*), max(c.bairro) from " + TABELA_CEPS_DNE
                                        + " c where c.cidade = :cidade and c.cep = :cep"
                        )
                        .setParameter("cidade", cidadeCorreios)
                        .setParameter("cep", cep)
                        .getResultList();

        if (linhas == null || linhas.isEmpty()) {
            return -1L;
        }

        Long qtd = toLong(linhas.get(0)[0]);

        if (qtd == null || qtd == 0L) {
            return -1L;
        }

        Long bairro = toLong(linhas.get(0)[1]);

        return bairro == null ? 0L : bairro;
    }

    /*
     * Cidade dos Correios do logradouro unificado. -1 quando o ID não existe.
     */
    public Long buscarCidadeCorreiosDoUnificado(Long idUnificado) {

        @SuppressWarnings("unchecked")
        List<Object> linhas =
                manager.createNativeQuery(
                                "select nvl(lu.cidade_correios, 0) from "
                                        + TABELA_UNIFICADO + " lu where lu.id = :id"
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
     * Grava cidade + distrito + logradouro da origem no logradouro unificado,
     * somente quando os três ainda estão vazios (null ou 0).
     *
     * Retorna 0 quando o logradouro unificado já possui outro código desta origem.
     */
    public int vincularLogradouro(
            OrigemLogradouro origem,
            Long idUnificado,
            LogradouroOrigemDados dados) {

        String sql = "update " + TABELA_UNIFICADO + " lu"
                + " set lu." + origem.getColunaCidade() + " = :cidade,"
                + " lu." + origem.getColunaDistrito() + " = :distrito,"
                + " lu." + origem.getColunaLogradouro() + " = :logradouro,"
                + " lu.data_alteracao = sysdate"
                + " where lu.id = :id"
                + "   and nvl(lu." + origem.getColunaCidade() + ", 0) = 0"
                + "   and nvl(lu." + origem.getColunaDistrito() + ", 0) = 0"
                + "   and nvl(lu." + origem.getColunaLogradouro() + ", 0) = 0";

        return manager.createNativeQuery(sql)
                .setParameter("cidade", dados.cidade())
                .setParameter("distrito", dados.distrito())
                .setParameter("logradouro", dados.logradouro())
                .setParameter("id", idUnificado)
                .executeUpdate();
    }

    /*
     * Completa as colunas configuradas em OrigemLogradouro.getComplementos()
     * com os dados do logradouro na origem, somente quando estiverem vazias.
     */
    public void complementarLogradouro(
            OrigemLogradouro origem,
            Long idUnificado,
            LogradouroOrigemDados dados) {

        if (origem.getComplementos().isEmpty()) {
            return;
        }

        StringBuilder sets = new StringBuilder();

        for (OrigemLogradouro.Complemento complemento : origem.getComplementos()) {

            String coluna = "lu." + complemento.coluna();

            String valorOrigem = "(select " + complemento.expressao()
                    + " from " + origem.getTabelaLogradourosCompleta() + " o"
                    + " where o.cidade = :cidade"
                    + " and o.distrito = :distrito"
                    + " and o.logradouro = :logradouro"
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

        String sql = "update " + TABELA_UNIFICADO + " lu"
                + " set " + sets
                + " where lu.id = :id";

        manager.createNativeQuery(sql)
                .setParameter("cidade", dados.cidade())
                .setParameter("distrito", dados.distrito())
                .setParameter("logradouro", dados.logradouro())
                .setParameter("id", idUnificado)
                .executeUpdate();
    }

    /*
     * status D: código duplicado da origem, vinculado a idUnificado;
     * status N: não vincular.
     */
    public void registrarControle(
            OrigemLogradouro origem,
            Long cidadeOrigem,
            Long distritoOrigem,
            Long logradouroOrigem,
            String status,
            Long idUnificado,
            String motivo,
            String usuario) {

        manager.createNativeQuery("merge into " + TABELA_CTRL + " destino"
                        + " using"
                        + " ("
                        + "     select :origem as origem,"
                        + "            :cidadeOrigem as cidade_origem,"
                        + "            :distritoOrigem as distrito_origem,"
                        + "            :logradouroOrigem as logradouro_origem"
                        + "       from dual"
                        + " ) origem"
                        + " on"
                        + " ("
                        + "     destino.ORIGEM = origem.origem"
                        + "     and destino.CIDADE_ORIGEM = origem.cidade_origem"
                        + "     and destino.DISTRITO_ORIGEM = origem.distrito_origem"
                        + "     and destino.LOGRADOURO_ORIGEM = origem.logradouro_origem"
                        + " )"
                        + " when matched then"
                        + "     update set"
                        + "         destino.STATUS = :status,"
                        + "         destino.ID_UNIFICADO = :idUnificado,"
                        + "         destino.MOTIVO = :motivo,"
                        + "         destino.USUARIO = :usuario,"
                        + "         destino.DATA_REGISTRO = sysdate"
                        + " when not matched then"
                        + "     insert"
                        + "     ("
                        + "         ID, ORIGEM, CIDADE_ORIGEM, DISTRITO_ORIGEM, LOGRADOURO_ORIGEM,"
                        + "         STATUS, ID_UNIFICADO, MOTIVO, USUARIO, DATA_REGISTRO"
                        + "     )"
                        + "     values"
                        + "     ("
                        + "         " + SCHEMA + ".SEQ_LOGRADOUROS_UNIF_CTRL.nextval,"
                        + "         :origem, :cidadeOrigem, :distritoOrigem, :logradouroOrigem,"
                        + "         :status, :idUnificado, :motivo, :usuario, sysdate"
                        + "     )")
                .setParameter("origem", origem.name())
                .setParameter("cidadeOrigem", cidadeOrigem)
                .setParameter("distritoOrigem", distritoOrigem)
                .setParameter("logradouroOrigem", logradouroOrigem)
                .setParameter("status", status)
                .setParameter("idUnificado", idUnificado)
                .setParameter("motivo", motivo)
                .setParameter("usuario", usuario)
                .executeUpdate();
    }

    /*
     * Cria o logradouro que não existe na base dos Correios:
     *
     * 1. DBO_CORREIOS.LOGRADOUROS no distrito dos Correios, STATUS = 'N',
     *    código pela sequence (nunca MAX + 1), bairro do CEP escolhido;
     * 2. DBO_CORREIOS.CEPS, STATUS = 'N', com o CEP escolhido — um CEP que já
     *    existe na tabela de CEPs. O que se cria é só o vínculo do CEP com o
     *    novo código de logradouro (mesmo princípio da SP_CARGA_CEPS);
     * 3. DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO com o código desta origem
     *    e zero nas demais.
     *
     * Os complementos são gravados depois, por complementarLogradouro.
     * Retorna o ID gerado em LOGRADOUROS_UNIFICADO.
     */
    public Long criarLogradouro(
            OrigemLogradouro origem,
            LogradouroOrigemDados dados,
            Long cep,
            Long bairroCorreios) {

        Long logradouroCorreios = nextVal(SEQ_CORREIOS);

        manager.createNativeQuery(
                        "insert into " + TABELA_CORREIOS
                                + " (CIDADE, DISTRITO, LOGRADOURO, BAIRRO, TIPO_LOGRADOURO,"
                                + " NOME, NOME_COMPLETO, NOME_TRANSF, TIPO_LOGRADOURO_TRANSF,"
                                + " NOME_REDUZIDO, STATUS)"
                                + " values (:cidade, :distrito, :logradouro, :bairro,"
                                + " substr(upper(trim(:tipo)), 1, 30),"
                                + " substr(upper(trim(:nome)), 1, 80),"
                                + " substr(upper(trim(:nome)), 1, 80),"
                                + " " + FN_TRANSF + "(upper(trim(:nome))),"
                                + " " + FN_TRANSF + "(upper(trim(:tipo))),"
                                + " substr(upper(trim(:nome)), 1, 150),"
                                + " 'N')"
                )
                .setParameter("cidade", dados.cidadeCorreios())
                .setParameter("distrito", dados.distritoCorreios())
                .setParameter("logradouro", logradouroCorreios)
                .setParameter("bairro", bairroCorreios)
                .setParameter("tipo", nvl(dados.tipo()))
                .setParameter("nome", nvl(dados.nome()))
                .executeUpdate();

        manager.createNativeQuery(
                        "insert into " + TABELA_CEPS_CORREIOS
                                + " (CIDADE, DISTRITO, LOGRADOURO, CEP, NUMERO_INI, NUMERO_FIN,"
                                + " IDENTIFICACAO, STATUS)"
                                + " values (:cidade, :distrito, :logradouro, :cep, null, null, null, 'N')"
                )
                .setParameter("cidade", dados.cidadeCorreios())
                .setParameter("distrito", dados.distritoCorreios())
                .setParameter("logradouro", logradouroCorreios)
                .setParameter("cep", cep)
                .executeUpdate();

        Long idUnificado = nextVal(SEQ_UNIFICADO);

        StringBuilder colunasOrigem = new StringBuilder();
        StringBuilder valoresOrigem = new StringBuilder();

        for (OrigemLogradouro item : OrigemLogradouro.values()) {

            colunasOrigem.append(item.getColunaCidade()).append(", ")
                    .append(item.getColunaDistrito()).append(", ")
                    .append(item.getColunaLogradouro()).append(", ");

            if (item == origem) {
                valoresOrigem.append(":cidadeOrigem, :distritoOrigem, :logradouroOrigem, ");
            } else {
                valoresOrigem.append("0, 0, 0, ");
            }
        }

        String sql = "insert into " + TABELA_UNIFICADO
                + " (ID, CIDADE_CORREIOS, DISTRITO_CORREIOS, LOGRADOURO_CORREIOS, "
                + colunasOrigem
                + "BAIRRO, TIPO_LOGRADOURO, NOME, NOME_CORREIOS, NOME_REDUZIDO, CEP,"
                + " CHAVE_NOME, CHAVE_TIPO,"
                + " DATA_ALTERACAO, USUARIO_ALTERACAO, STATUS_COR)"
                + " values (:id, :cidadeCorreios, :distritoCorreios, :logradouroCorreios, "
                + valoresOrigem
                + ":bairro, substr(upper(trim(:tipo)), 1, 30),"
                + " substr(upper(trim(:nome)), 1, 80), substr(upper(trim(:nome)), 1, 80),"
                + " substr(upper(trim(:nome)), 1, 90), :cep,"
                + " " + PKG_CHAVE + ".chave(null, null, :nome),"
                + " " + PKG_CHAVE + ".tipo(:tipo),"
                + " sysdate, null, :statusCor)";

        manager.createNativeQuery(sql)
                .setParameter("id", idUnificado)
                .setParameter("cidadeCorreios", dados.cidadeCorreios())
                .setParameter("distritoCorreios", dados.distritoCorreios())
                .setParameter("logradouroCorreios", logradouroCorreios)
                .setParameter("cidadeOrigem", dados.cidade())
                .setParameter("distritoOrigem", dados.distrito())
                .setParameter("logradouroOrigem", dados.logradouro())
                .setParameter("bairro", bairroCorreios)
                .setParameter("tipo", nvl(dados.tipo()))
                .setParameter("nome", nvl(dados.nome()))
                .setParameter("cep", cep)
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

    private String sqlLote(OrigemLogradouro origem) {

        LogradouroUnificadoFilter filter = new LogradouroUnificadoFilter();

        return " select c.cidade_origem, c.distrito_origem, c.logradouro_origem, c.id_sugerido,"
                + " row_number() over (partition by c.id_sugerido"
                + "   order by c.cidade_origem, c.distrito_origem, c.logradouro_origem) as rn"
                + " from (" + sqlClassificacao(origem, filter) + ") c"
                + " where c.situacao in ('UNICA', 'FAIXAS')"
                + "   and c.id_sugerido is not null";
    }

    public long registrarDuplicadosDoLote(OrigemLogradouro origem) {

        String sql = "insert into " + TABELA_CTRL
                + " (ID, ORIGEM, CIDADE_ORIGEM, DISTRITO_ORIGEM, LOGRADOURO_ORIGEM,"
                + " STATUS, ID_UNIFICADO, MOTIVO, USUARIO, DATA_REGISTRO)"
                + " select " + SCHEMA + ".SEQ_LOGRADOUROS_UNIF_CTRL.nextval,"
                + " :origem, l.cidade_origem, l.distrito_origem, l.logradouro_origem,"
                + " '" + STATUS_DUPLICADO + "', l.id_sugerido,"
                + " 'Código duplicado na origem (lote).', null, sysdate"
                + " from (" + sqlLote(origem) + ") l"
                + " where l.rn > 1";

        return manager.createNativeQuery(sql)
                .setParameter("origem", origem.name())
                .executeUpdate();
    }

    public long vincularLote(OrigemLogradouro origem) {

        String cid = "u." + origem.getColunaCidade();
        String dist = "u." + origem.getColunaDistrito();
        String cod = "u." + origem.getColunaLogradouro();

        String sql = "merge into " + TABELA_UNIFICADO + " u"
                + " using (select * from (" + sqlLote(origem) + ") where rn = 1) s"
                + " on (u.id = s.id_sugerido)"
                + " when matched then update set"
                + " " + cid + " = s.cidade_origem,"
                + " " + dist + " = s.distrito_origem,"
                + " " + cod + " = s.logradouro_origem,"
                + " u.data_alteracao = sysdate"
                + " where nvl(" + cid + ", 0) = 0"
                + "   and nvl(" + dist + ", 0) = 0"
                + "   and nvl(" + cod + ", 0) = 0";

        return manager.createNativeQuery(sql)
                .setParameter("origem", origem.name())
                .executeUpdate();
    }

    public long complementarVinculados(OrigemLogradouro origem) {

        if (origem.getComplementos().isEmpty()) {
            return 0L;
        }

        String cid = "u." + origem.getColunaCidade();
        String dist = "u." + origem.getColunaDistrito();
        String cod = "u." + origem.getColunaLogradouro();

        StringBuilder sets = new StringBuilder();
        StringBuilder vazios = new StringBuilder();

        for (OrigemLogradouro.Complemento complemento : origem.getComplementos()) {

            String coluna = "u." + complemento.coluna();

            String valorOrigem = "(select " + complemento.expressao()
                    + " from " + origem.getTabelaLogradourosCompleta() + " o"
                    + " where o.cidade = " + cid
                    + " and o.distrito = " + dist
                    + " and o.logradouro = " + cod
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
     * Mesmo critério da tela de bairros (DISTRITOS_UNIFICADO + controle D).
     */
    private String sqlDeParaDistrito(OrigemLogradouro origem) {

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

    /*
     * Descrição do tipo na tabela de tipos da origem; sem cadastro, o próprio código.
     */
    private String sqlTipoOrigem(OrigemLogradouro origem) {
        return "nvl((select max(tp.descricao) from " + origem.getTabelaTiposCompleta()
                + " tp where tp.tipo_logradouro = o.tipo_logradouro), o.tipo_logradouro)";
    }

    private String sqlUfCidadeOrigem(OrigemLogradouro origem) {
        return "(select max(upper(trim(c.estado))) from "
                + origem.getTabelaCidadesCompleta()
                + " c where c.cidade = o.cidade)";
    }

    private String sqlNomeCidadeOrigem(OrigemLogradouro origem) {
        return "(select max(c.nome) from "
                + origem.getTabelaCidadesCompleta()
                + " c where c.cidade = o.cidade)";
    }

    /*
     * Tipo compatível: iguais, ou um dos lados sem tipo.
     */
    private String sqlTipoCompativel(String tipoUnificado, String tipoOrigem) {
        return "(" + tipoUnificado + " is null or " + tipoOrigem + " is null"
                + " or " + tipoUnificado + " = " + tipoOrigem + ")";
    }

    private String condicaoChave(OrigemLogradouro origem, String alias) {
        return alias + "." + origem.getColunaCidade() + " = o.cidade"
                + " and " + alias + "." + origem.getColunaDistrito() + " = o.distrito"
                + " and " + alias + "." + origem.getColunaLogradouro() + " = o.logradouro";
    }

    private String sqlCodigoAtual(OrigemLogradouro origem, String alias) {
        return "greatest(nvl(" + alias + "." + origem.getColunaCidade() + ", 0),"
                + " nvl(" + alias + "." + origem.getColunaDistrito() + ", 0),"
                + " nvl(" + alias + "." + origem.getColunaLogradouro() + ", 0))";
    }

    /*
     * Logradouros da origem ainda não tratados, com a chave do nome e do tipo.
     */
    private String sqlOrigemPendente(
            OrigemLogradouro origem,
            LogradouroUnificadoFilter filter) {

        StringBuilder sql = new StringBuilder();

        sql.append(" select t.cidade_origem,")
                .append(" t.distrito_origem,")
                .append(" t.logradouro_origem,")
                .append(" t.tipo,")
                .append(" t.nome,")
                .append(" t.uf,")
                .append(" t.nome_cidade,")
                .append(" ").append(PKG_CHAVE).append(".chave(null, null, t.nome) as chave_nome,")
                .append(" ").append(PKG_CHAVE).append(".tipo(t.tipo) as chave_tipo,")
                .append(" t.cidade_correios,")
                .append(" t.distrito_correios,")
                .append(" t.qtd_distrito")
                .append(" from (")
                .append(" select o.cidade as cidade_origem,")
                .append(" o.distrito as distrito_origem,")
                .append(" o.logradouro as logradouro_origem,")
                .append(" ").append(sqlTipoOrigem(origem)).append(" as tipo,")
                .append(" o.nome as nome,")
                .append(" ").append(sqlUfCidadeOrigem(origem)).append(" as uf,")
                .append(" ").append(sqlNomeCidadeOrigem(origem)).append(" as nome_cidade,")
                .append(" case when dd.qtd_distrito = 1 then dd.cidade_correios end as cidade_correios,")
                .append(" case when dd.qtd_distrito = 1 then dd.distrito_correios end as distrito_correios,")
                .append(" nvl(dd.qtd_distrito, 0) as qtd_distrito")
                .append(" from ").append(origem.getTabelaLogradourosCompleta()).append(" o")
                .append(" left join (").append(sqlDeParaDistrito(origem)).append(") dd")
                .append("   on dd.cid = o.cidade")
                .append("  and dd.dist = o.distrito")
                .append(" where nvl(o.cidade, 0) > 0")
                .append("   and nvl(o.logradouro, 0) > 0")
                .append("   and not exists (select 1 from ").append(TABELA_UNIFICADO)
                .append("        x where ").append(condicaoChave(origem, "x")).append(")")
                .append("   and not exists (select 1 from ").append(TABELA_CTRL)
                .append("        k where k.origem = :origem")
                .append("        and k.cidade_origem = o.cidade")
                .append("        and k.distrito_origem = o.distrito")
                .append("        and k.logradouro_origem = o.logradouro)");

        if (filter.getCidade() != null) {
            sql.append(" and o.cidade = :cidade");
        }

        if (filter.getDistrito() != null) {
            sql.append(" and o.distrito = :distrito");
        }

        if (filter.getLogradouro() != null) {
            sql.append(" and o.logradouro = :logradouro");
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
     * Classificação em uma única passada pela origem:
     *
     * 1. junta cada pendente com os logradouros unificados de mesma chave
     *    no mesmo distrito dos Correios (índice CIDADE/DISTRITO/CHAVE_NOME);
     * 2. agrupa por pendente contando candidatos, candidatos de tipo compatível
     *    e candidatos que NÃO são faixa de numeração;
     * 3. o candidato principal é o de tipo compatível, sem faixa no complemento,
     *    de menor código dos Correios.
     */
    private String sqlClassificacao(
            OrigemLogradouro origem,
            LogradouroUnificadoFilter filter) {

        String pendentes = sqlOrigemPendente(origem, filter);

        String unificado = " select lu.id,"
                + " lu.cidade_correios,"
                + " lu.distrito_correios,"
                + " lu.logradouro_correios,"
                + " lu.chave_nome,"
                + " lu.chave_tipo,"
                + " " + String.format(SQL_FAIXA, "lu") + " as faixa,"
                + " " + sqlCodigoAtual(origem, "lu") + " as cod_atual"
                + " from " + TABELA_UNIFICADO + " lu"
                + " where nvl(lu.cidade_correios, 0) > 0"
                + "   and lu.chave_nome is not null";

        String juncao = " select po.*,"
                + " un.id as un_id,"
                + " un.logradouro_correios as un_logradouro,"
                + " un.faixa as un_faixa,"
                + " un.cod_atual as un_cod_atual,"
                + " case when un.id is not null"
                + "       and " + sqlTipoCompativel("un.chave_tipo", "po.chave_tipo")
                + "      then 1 else 0 end as tipo_ok"
                + " from (" + pendentes + ") po"
                + " left join (" + unificado + ") un"
                + "   on po.qtd_distrito = 1"
                + "  and un.cidade_correios = po.cidade_correios"
                + "  and un.distrito_correios = po.distrito_correios"
                + "  and un.chave_nome = po.chave_nome";

        String ordemPrincipal =
                " order by 1 - j.tipo_ok, j.un_faixa, j.un_logradouro, j.un_id";

        String agrupado = " select j.cidade_origem,"
                + " j.distrito_origem,"
                + " j.logradouro_origem,"
                + " j.tipo,"
                + " j.nome,"
                + " j.uf,"
                + " j.nome_cidade,"
                + " j.cidade_correios,"
                + " j.distrito_correios,"
                + " j.qtd_distrito,"
                + " count(j.un_id) as qtd_nome,"
                + " sum(j.tipo_ok) as qtd_tipo,"
                + " sum(case when j.tipo_ok = 1 and j.un_faixa = 0 then 1 else 0 end) as qtd_nao_faixa,"
                + " min(j.un_id) keep (dense_rank first" + ordemPrincipal + ") as id_principal,"
                + " max(j.un_cod_atual) keep (dense_rank first" + ordemPrincipal + ") as ocupado_principal"
                + " from (" + juncao + ") j"
                + " group by j.cidade_origem, j.distrito_origem, j.logradouro_origem,"
                + " j.tipo, j.nome, j.uf, j.nome_cidade,"
                + " j.cidade_correios, j.distrito_correios, j.qtd_distrito";

        return " select g.cidade_origem,"
                + " g.distrito_origem,"
                + " g.logradouro_origem,"
                + " g.tipo,"
                + " g.nome,"
                + " g.uf,"
                + " g.nome_cidade,"
                + " g.cidade_correios,"
                + " g.distrito_correios,"
                + " case when g.qtd_tipo > 0 then g.qtd_tipo else g.qtd_nome end as qtd_candidatos,"
                + " case when g.qtd_tipo > 0 and (g.qtd_tipo = 1 or g.qtd_nao_faixa <= 1)"
                + "      then g.id_principal end as id_sugerido,"
                + " case"
                + "   when g.qtd_distrito <> 1 then 'DISTRITO_NAO_UNIFICADO'"
                + "   when g.qtd_nome = 0 then 'SEM_CORRESPONDENCIA'"
                + "   when g.qtd_tipo = 0 then 'TIPO_DIFERENTE'"
                + "   when g.qtd_tipo > 1 and g.qtd_nao_faixa > 1 then 'AMBIGUA'"
                + "   when nvl(g.ocupado_principal, 0) > 0 then 'DUPLICADA'"
                + "   when g.qtd_tipo = 1 then 'UNICA'"
                + "   else 'FAIXAS'"
                + " end as situacao"
                + " from (" + agrupado + ") g";
    }

    private void aplicarParametros(
            OrigemLogradouro origem,
            LogradouroUnificadoFilter filter,
            Query query) {

        query.setParameter("origem", origem.name());

        if (filter.getCidade() != null) {
            query.setParameter("cidade", filter.getCidade());
        }

        if (filter.getDistrito() != null) {
            query.setParameter("distrito", filter.getDistrito());
        }

        if (filter.getLogradouro() != null) {
            query.setParameter("logradouro", filter.getLogradouro());
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

    private Long count(String sql, OrigemLogradouro origem) {

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

    private static Long nvl(Long valor) {
        return valor == null ? 0L : valor;
    }
}
