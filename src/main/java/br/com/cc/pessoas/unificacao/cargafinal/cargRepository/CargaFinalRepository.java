package br.com.cc.pessoas.unificacao.cargafinal.cargRepository;

import br.com.cc.pessoas.unificacao.cargafinal.cargEnum.NivelCargaFinal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/*
 * Carga final (fase 3): DBO_UNI_PESSOAS (consolidada) -> DBO_CC_ENDERECOS e
 * DBO_CCM_PESSOAS, com o MESMO ID nos dois schemas, gravado de volta em
 * CODIGO_CCM da consolidada (como SP_CARGA_ENDERECOS e SP_CARGA_CEPS).
 *
 * Regras:
 *
 * - incremental: só recebe código novo quem está com CODIGO_CCM vazio (null ou 0);
 *   quem já tem código nunca muda (os endereços das pessoas apontam para eles);
 * - nunca apaga nada, em nenhum dos três schemas;
 * - linha com código mas ausente na tabela final é completada (reparo);
 * - SQL de conjunto, sem laço: um nível inteiro roda em uma transação;
 * - cada nível só carrega o que tem o nível de cima já carregado; o resto
 *   aparece como "bloqueado" no resumo, sem interromper a carga.
 *
 * Correções em relação às procedures:
 *
 * - sem o filtro de teste upper(nome) like 'bairro 9%' (bairros);
 * - sem a cidade 6160 fixa no código (CEPs);
 * - tipo de logradouro pela tabela TIPOS_LOGRADOUROS (descrição normalizada),
 *   não por INSTR com IDs fixos (RODOVIA virava RUA, POV virava PRAÇA);
 * - bairro de mesmo nome no mesmo distrito reaproveita o ID existente
 *   (a procedure pulava e deixava o bairro sem CODIGO_CCM);
 * - CEP: a procedure pulava qualquer CEP cujo número já existisse, mesmo em
 *   outro logradouro; aqui a comparação é por logradouro + CEP + número inicial;
 * - nenhum "exit": um registro sem pai não interrompe a carga.
 *
 * Sequences: as mesmas das procedures (DBO_CC_ENDERECOS.SEQ_DISTRITOS, SEQ_BAIRROS,
 * SEQ_DADOS_BAIRROS, SEQ_LOGRADOUROS, SEQ_DADOS_LOGRADOUROS, SEQ_LOGRADOUROS_CEPS,
 * SEQ_BAIRROS_CEPS e DBO_CCM_PESSOAS.SEQ_CEPS).
 */
@Repository
public class CargaFinalRepository {

    private static final String U = "DBO_UNI_PESSOAS";
    private static final String E = "DBO_CC_ENDERECOS";
    private static final String M = "DBO_CCM_PESSOAS";

    private static final String PKG_TIPO = U + ".PKG_LOGRADOURO_CHAVE.tipo";

    @PersistenceContext
    private EntityManager manager;

    /*
     * Contagens de um nível: [consolidado, comCodigo, pendentes, bloqueados,
     * faltandoCcEnderecos, faltandoCcmPessoas].
     */
    public long[] contar(NivelCargaFinal nivel) {

        return switch (nivel) {

            case CIDADES -> new long[]{
                    count("select count(*) from " + U + ".CIDADES"),
                    count("select count(*) from " + U + ".CIDADES"),
                    count("select count(*) from " + U + ".CIDADES c where " + EXISTE_ESTADO
                            + " and (not exists (select 1 from " + E + ".CIDADES f where f.id = c.cidade)"
                            + " or not exists (select 1 from " + M + ".CIDADES f where f.id = c.cidade))"),
                    count("select count(*) from " + U + ".CIDADES c where not " + EXISTE_ESTADO),
                    count("select count(*) from " + U + ".CIDADES c"
                            + " where not exists (select 1 from " + E + ".CIDADES f where f.id = c.cidade)"),
                    count("select count(*) from " + U + ".CIDADES c"
                            + " where not exists (select 1 from " + M + ".CIDADES f where f.id = c.cidade)")
            };

            case DISTRITOS -> contarComCodigo(
                    U + ".DISTRITOS",
                    " exists (select 1 from " + E + ".CIDADES f where f.id = x.cidade) and x.nome is not null",
                    "x",
                    E + ".DISTRITOS",
                    M + ".DISTRITOS"
            );

            case BAIRROS -> contarComCodigo(
                    U + ".BAIRROS",
                    " x.nome is not null and " + existeDistritoFinal("x"),
                    "x",
                    E + ".BAIRROS",
                    M + ".BAIRROS"
            );

            case LOGRADOUROS -> contarComCodigo(
                    U + ".LOGRADOUROS",
                    " nvl(x.cidade, 0) > 0 and " + existeDistritoFinal("x"),
                    "x",
                    E + ".LOGRADOUROS",
                    M + ".LOGRADOUROS"
            );

            case CEPS -> {
                long total = count("select count(*) from " + U + ".CEPS");
                long com = count("select count(*) from " + U + ".CEPS where codigo_ccm > 0");
                long pend = count("select count(*) from (" + sqlCepsComPais() + ") s"
                        + " where nvl(s.codigo_ccm, 0) = 0 and s.lid > 0 and s.bid > 0");
                long semCodigo = total - com;
                yield new long[]{
                        total,
                        com,
                        pend,
                        Math.max(semCodigo - pend, 0L),
                        count("select count(*) from (select distinct codigo_ccm from " + U + ".CEPS"
                                + " where codigo_ccm > 0) x"
                                + " where not exists (select 1 from " + E + ".CEPS f where f.id = x.codigo_ccm)"),
                        count("select count(*) from (select distinct codigo_ccm from " + U + ".CEPS"
                                + " where codigo_ccm > 0) x"
                                + " where not exists (select 1 from " + M + ".CEPS f where f.id = x.codigo_ccm)")
                };
            }
        };
    }

    /**********************************************************************
     * CIDADES — o ID é o próprio código da cidade (Correios)
     **********************************************************************/

    private static final String EXISTE_ESTADO =
            "exists (select 1 from " + E + ".ESTADOS e where e.sigla = c.estado)";

    private static final String ESTADO_ID =
            "(select min(e.id) from " + E + ".ESTADOS e where e.sigla = c.estado)";

    public long[] carregarCidades() {

        long e = exec("insert into " + E + ".CIDADES"
                + " (ID, ESTADO, NOME, CODIGO_SICOM, CODIGO_IBGE, CODIGO_INEP, USUARIO, DATA_ALTERACAO)"
                + " select c.cidade, " + ESTADO_ID + ", c.nome, c.cidade_sicom, c.codigo_ibge,"
                + " c.cidade_inep, c.usuario_alteracao, nvl(c.data_alteracao, sysdate)"
                + " from " + U + ".CIDADES c"
                + " where " + EXISTE_ESTADO
                + " and not exists (select 1 from " + E + ".CIDADES f where f.id = c.cidade)");

        long m = exec("insert into " + M + ".CIDADES"
                + " (ID, ESTADO_ID, NOME, CODIGO_SICOM, CODIGO_IBGE, CODIGO_INEP, CEP)"
                + " select c.cidade, " + ESTADO_ID + ", c.nome, c.cidade_sicom, c.codigo_ibge,"
                + " c.cidade_inep, c.cep"
                + " from " + U + ".CIDADES c"
                + " where " + EXISTE_ESTADO
                + " and not exists (select 1 from " + M + ".CIDADES f where f.id = c.cidade)");

        return new long[]{0L, 0L, e, m};
    }

    /**********************************************************************
     * DISTRITOS
     **********************************************************************/

    public long[] carregarDistritos() {

        long novos = exec("merge into " + U + ".DISTRITOS u"
                + " using (select x.rowid as rid from " + U + ".DISTRITOS x"
                + "         where nvl(x.codigo_ccm, 0) = 0"
                + "           and x.nome is not null"
                + "           and exists (select 1 from " + E + ".CIDADES f where f.id = x.cidade)) s"
                + " on (u.rowid = s.rid)"
                + " when matched then update set u.codigo_ccm = " + E + ".SEQ_DISTRITOS.nextval");

        String origem = "select s.* from (select x.*, row_number() over"
                + " (partition by x.codigo_ccm order by x.cidade, x.distrito) as rn"
                + " from " + U + ".DISTRITOS x where x.codigo_ccm > 0) s where s.rn = 1";

        long e = exec("insert into " + E + ".DISTRITOS (ID, CIDADE, NOME, CODIGO_INEP, USUARIO, DATA_ALTERACAO)"
                + " select s.codigo_ccm, s.cidade, s.nome, s.distrito_inep, null, sysdate"
                + " from (" + origem + ") s"
                + " where exists (select 1 from " + E + ".CIDADES f where f.id = s.cidade)"
                + " and not exists (select 1 from " + E + ".DISTRITOS f where f.id = s.codigo_ccm)");

        long m = exec("insert into " + M + ".DISTRITOS (ID, CIDADE_ID, NOME, CODIGO_INEP)"
                + " select s.codigo_ccm, s.cidade, s.nome, s.distrito_inep"
                + " from (" + origem + ") s"
                + " where exists (select 1 from " + E + ".CIDADES f where f.id = s.cidade)"
                + " and not exists (select 1 from " + M + ".DISTRITOS f where f.id = s.codigo_ccm)");

        return new long[]{novos, 0L, e, m};
    }

    /**********************************************************************
     * BAIRROS (+ DADOS_BAIRROS)
     **********************************************************************/

    public long[] carregarBairros() {

        // 1. mesmo nome no mesmo distrito final: reaproveita o ID que já existe
        long reaproveitados = exec("merge into " + U + ".BAIRROS u"
                + " using (select x.rowid as rid, min(fb.id) as id"
                + "          from " + U + ".BAIRROS x"
                + "          join " + U + ".DISTRITOS d on d.cidade = x.cidade and d.distrito = x.distrito"
                + "          join " + E + ".BAIRROS fb on fb.distrito = d.codigo_ccm and fb.nome = x.nome"
                + "         where nvl(x.codigo_ccm, 0) = 0"
                + "           and d.codigo_ccm > 0"
                + "         group by x.rowid) s"
                + " on (u.rowid = s.rid)"
                + " when matched then update set u.codigo_ccm = s.id");

        // 2. código novo: um por cidade + distrito + nome
        long novos = exec("merge into " + U + ".BAIRROS u"
                + " using (select rid from ("
                + "          select x.rowid as rid,"
                + "                 row_number() over (partition by x.cidade, x.distrito, x.nome"
                + "                                    order by x.bairro) as rn"
                + "            from " + U + ".BAIRROS x"
                + "           where nvl(x.codigo_ccm, 0) = 0"
                + "             and x.nome is not null"
                + "             and " + existeDistritoFinal("x")
                + "        ) where rn = 1) s"
                + " on (u.rowid = s.rid)"
                + " when matched then update set u.codigo_ccm = " + E + ".SEQ_BAIRROS.nextval");

        // 3. os demais de mesmo nome recebem o mesmo código
        reaproveitados += exec("merge into " + U + ".BAIRROS u"
                + " using (select x.rowid as rid, max(y.codigo_ccm) as id"
                + "          from " + U + ".BAIRROS x"
                + "          join " + U + ".BAIRROS y"
                + "            on y.cidade = x.cidade and y.distrito = x.distrito"
                + "           and y.nome = x.nome and y.codigo_ccm > 0"
                + "         where nvl(x.codigo_ccm, 0) = 0"
                + "         group by x.rowid) s"
                + " on (u.rowid = s.rid)"
                + " when matched then update set u.codigo_ccm = s.id");

        String origem = "select s.* from (select x.*, d.codigo_ccm as dist_ccm,"
                + " row_number() over (partition by x.codigo_ccm order by x.cidade, x.distrito, x.bairro) as rn"
                + " from " + U + ".BAIRROS x"
                + " join " + U + ".DISTRITOS d on d.cidade = x.cidade and d.distrito = x.distrito"
                + " where x.codigo_ccm > 0) s where s.rn = 1";

        long e = exec("insert into " + E + ".BAIRROS (ID, DISTRITO, NOME, NOME_ABREVIADO, USUARIO, DATA_ALTERACAO)"
                + " select s.codigo_ccm, s.dist_ccm, s.nome, substr(s.nome_abreviado, 1, 100),"
                + " s.usuario_alteracao, nvl(s.data_alteracao, sysdate)"
                + " from (" + origem + ") s"
                + " where exists (select 1 from " + E + ".DISTRITOS f where f.id = s.dist_ccm)"
                + " and not exists (select 1 from " + E + ".BAIRROS f where f.id = s.codigo_ccm)");

        long m = exec("insert into " + M + ".BAIRROS (ID, DISTRITO_ID, NOME, NOME_ABREVIADO)"
                + " select s.codigo_ccm, s.dist_ccm, s.nome, substr(s.nome_abreviado, 1, 100)"
                + " from (" + origem + ") s"
                + " where exists (select 1 from " + M + ".DISTRITOS f where f.id = s.dist_ccm)"
                + " and not exists (select 1 from " + M + ".BAIRROS f where f.id = s.codigo_ccm)");

        String dados = " (ID, BAIRRO, DATA_CRIACAO, LEI_CRIACAO, DATA_DECRETO, DECRETO,"
                + " DATA_PORTARIA, PORTARIA, PESSOA_LOTEADORA, ZONA_RURAL)"
                + " select " + E + ".SEQ_DADOS_BAIRROS.nextval, s.codigo_ccm, s.data_criacao,"
                + " s.lei_criacao, s.data_decreto, s.decreto, s.data_portaria, s.portaria,"
                + " s.pessoa_loteadora, substr(s.zona_rural, 1, 1)"
                + " from (" + origem + ") s";

        exec("insert into " + E + ".DADOS_BAIRROS" + dados
                + " where exists (select 1 from " + E + ".BAIRROS f where f.id = s.codigo_ccm)"
                + " and not exists (select 1 from " + E + ".DADOS_BAIRROS f where f.bairro = s.codigo_ccm)");

        exec("insert into " + M + ".DADOS_BAIRROS" + dados
                + " where exists (select 1 from " + M + ".BAIRROS f where f.id = s.codigo_ccm)"
                + " and not exists (select 1 from " + M + ".DADOS_BAIRROS f where f.bairro = s.codigo_ccm)");

        return new long[]{novos, reaproveitados, e, m};
    }

    /**********************************************************************
     * LOGRADOUROS (+ DADOS_LOGRADOUROS)
     **********************************************************************/

    /*
     * Tipo pela descrição normalizada (PKG_LOGRADOURO_CHAVE.TIPO, o mesmo da
     * unificação): primeiro TIPO_LOGRAD_DESC ('RUA', 'AVENIDA'...), depois o
     * código TIPO_LOGRADOURO, depois 'NEC'. As subconsultas dependem de poucos
     * valores distintos e ficam em cache durante o comando.
     */
    private static final String TIPO_ID =
            "nvl((select min(t.id) from " + E + ".TIPOS_LOGRADOUROS t"
                    + "      where " + PKG_TIPO + "(t.descricao) = " + PKG_TIPO + "(s.tipo_lograd_desc)),"
                    + " nvl((select min(t.id) from " + E + ".TIPOS_LOGRADOUROS t"
                    + "      where " + PKG_TIPO + "(t.descricao) = " + PKG_TIPO + "(s.tipo_logradouro)"
                    + "         or upper(trim(t.sigla)) = upper(trim(s.tipo_logradouro))),"
                    + " (select min(t.id) from " + E + ".TIPOS_LOGRADOUROS t"
                    + "   where upper(trim(t.sigla)) = 'NEC')))";

    public long[] carregarLogradouros() {

        long novos = exec("merge into " + U + ".LOGRADOUROS u"
                + " using (select x.rowid as rid from " + U + ".LOGRADOUROS x"
                + "         where nvl(x.codigo_ccm, 0) = 0"
                + "           and nvl(x.cidade, 0) > 0"
                + "           and " + existeDistritoFinal("x") + ") s"
                + " on (u.rowid = s.rid)"
                + " when matched then update set u.codigo_ccm = " + E + ".SEQ_LOGRADOUROS.nextval");

        String origem = "select x.*, d.codigo_ccm as dist_ccm"
                + " from " + U + ".LOGRADOUROS x"
                + " join " + U + ".DISTRITOS d on d.cidade = x.cidade and d.distrito = x.distrito"
                + " where x.codigo_ccm > 0";

        String colunas = " s.codigo_ccm, s.dist_ccm, " + TIPO_ID + ","
                + " substr(nvl(s.nome, nvl(s.nome1, 'NOME')), 1, 255),"
                + " substr(s.preposicao, 1, 10), substr(s.titulo_patente, 1, 50),"
                + " substr(s.nome_reduzido, 1, 100), substr(s.nome1, 1, 100),"
                + " substr(s.complemento, 1, 100)";

        long e = exec("insert into " + E + ".LOGRADOUROS (ID, DISTRITO, TIPO_LOGRADOURO, NOME, PREPOSICAO,"
                + " TITULO_PATENTE, NOME_REDUZIDO, NOME_SIMPLIFICADO, COMPLEMENTO, USUARIO, DATA_ALTERACAO)"
                + " select" + colunas + ", s.usuario_alteracao, nvl(s.data_alteracao, sysdate)"
                + " from (" + origem + ") s"
                + " where exists (select 1 from " + E + ".DISTRITOS f where f.id = s.dist_ccm)"
                + " and not exists (select 1 from " + E + ".LOGRADOUROS f where f.id = s.codigo_ccm)");

        long m = exec("insert into " + M + ".LOGRADOUROS (ID, DISTRITO_ID, TIPO_LOGRADOURO_ID, NOME, PREPOSICAO,"
                + " TITULO_PATENTE, NOME_REDUZIDO, NOME_SIMPLIFICADO, COMPLEMENTO)"
                + " select" + colunas
                + " from (" + origem + ") s"
                + " where exists (select 1 from " + E + ".DISTRITOS f where f.id = s.dist_ccm)"
                + " and not exists (select 1 from " + M + ".LOGRADOUROS f where f.id = s.codigo_ccm)");

        exec("insert into " + E + ".DADOS_LOGRADOUROS (ID, LOGRADOURO)"
                + " select " + E + ".SEQ_DADOS_LOGRADOUROS.nextval, f.id"
                + " from " + E + ".LOGRADOUROS f"
                + " where not exists (select 1 from " + E + ".DADOS_LOGRADOUROS g where g.logradouro = f.id)");

        exec("insert into " + M + ".DADOS_LOGRADOUROS (ID, LOGRADOURO)"
                + " select " + E + ".SEQ_DADOS_LOGRADOUROS.nextval, f.id"
                + " from " + M + ".LOGRADOUROS f"
                + " where not exists (select 1 from " + M + ".DADOS_LOGRADOUROS g where g.logradouro = f.id)");

        return new long[]{novos, 0L, e, m};
    }

    /**********************************************************************
     * CEPS (+ LOGRADOUROS_CEPS e BAIRROS_CEPS, nos dois schemas)
     **********************************************************************/

    /*
     * CEP consolidado com o código final do logradouro (lid) e do bairro (bid).
     * Bairro: pelo código; se não tiver, pelo nome do bairro dos Correios no
     * mesmo distrito (mesma regra do PR_BUSCA_BAIRRO da SP_CARGA_CEPS).
     */
    private String sqlCepsComPais() {
        return "select u.rowid as rid, u.cidade, u.distrito, u.logradouro, u.bairro, u.cep,"
                + " u.numero_ini, u.numero_fim, u.identificacao, u.codigo_ccm,"
                + " l.codigo_ccm as lid,"
                + " nvl(b.codigo_ccm, bn.codigo_ccm) as bid"
                + " from " + U + ".CEPS u"
                + " left join " + U + ".LOGRADOUROS l"
                + "   on l.cidade = u.cidade and l.distrito = u.distrito and l.logradouro = u.logradouro"
                + "  and l.codigo_ccm > 0"
                + " left join " + U + ".BAIRROS b"
                + "   on b.cidade = u.cidade and b.distrito = u.distrito and b.bairro = u.bairro"
                + "  and b.codigo_ccm > 0"
                + " left join DBO_CORREIOS.BAIRROS cb"
                + "   on cb.cidade = u.cidade and cb.distrito = u.distrito and cb.bairro = u.bairro"
                + " left join (select cidade, distrito, nome, min(codigo_ccm) as codigo_ccm"
                + "              from " + U + ".BAIRROS where codigo_ccm > 0"
                + "             group by cidade, distrito, nome) bn"
                + "   on bn.cidade = u.cidade and bn.distrito = u.distrito and bn.nome = cb.nome";
    }

    public long[] carregarCeps() {

        // 1. CEP que já existe na tabela final para o mesmo logradouro: reaproveita o ID
        long reaproveitados = exec("merge into " + U + ".CEPS u"
                + " using (select p.rid, min(c.id) as id"
                + "          from (" + sqlCepsComPais() + ") p"
                + "          join " + M + ".CEPS c"
                + "            on c.logradouro_id = p.lid"
                + "           and trim(c.cep) = to_char(p.cep)"
                + "           and nvl(c.numero_ini, 0) = nvl(p.numero_ini, 0)"
                + "         where nvl(p.codigo_ccm, 0) = 0"
                + "         group by p.rid) s"
                + " on (u.rowid = s.rid)"
                + " when matched then update set u.codigo_ccm = s.id");

        // 2. código novo para os que têm logradouro e bairro carregados
        long novos = exec("merge into " + U + ".CEPS u"
                + " using (select p.rid from (" + sqlCepsComPais() + ") p"
                + "         where nvl(p.codigo_ccm, 0) = 0 and p.lid > 0 and p.bid > 0) s"
                + " on (u.rowid = s.rid)"
                + " when matched then update set u.codigo_ccm = " + M + ".SEQ_CEPS.nextval");

        String origem = "select s.* from (select p.*, row_number() over"
                + " (partition by p.codigo_ccm order by p.cidade, p.distrito, p.logradouro) as rn"
                + " from (" + sqlCepsComPais() + ") p"
                + " where p.codigo_ccm > 0 and p.lid > 0 and p.bid > 0) s where s.rn = 1";

        String valoresCep = " select s.codigo_ccm, s.lid, s.bid, to_char(s.cep),"
                + " s.numero_ini, s.numero_fim, s.identificacao, null"
                + " from (" + origem + ") s";

        long e = exec("insert into " + E + ".CEPS (ID, LOGRADOURO_ID, BAIRRO_ID, CEP, NUMERO_INI,"
                + " NUMERO_FIM, IDENTIFICACAO, CODIGO_CORREIOS)" + valoresCep
                + " where exists (select 1 from " + E + ".LOGRADOUROS f where f.id = s.lid)"
                + " and exists (select 1 from " + E + ".BAIRROS f where f.id = s.bid)"
                + " and not exists (select 1 from " + E + ".CEPS f where f.id = s.codigo_ccm)");

        long m = exec("insert into " + M + ".CEPS (ID, LOGRADOURO_ID, BAIRRO_ID, CEP, NUMERO_INI,"
                + " NUMERO_FIM, IDENTIFICACAO, CODIGO_CORREIOS_ID)" + valoresCep
                + " where exists (select 1 from " + M + ".LOGRADOUROS f where f.id = s.lid)"
                + " and exists (select 1 from " + M + ".BAIRROS f where f.id = s.bid)"
                + " and not exists (select 1 from " + M + ".CEPS f where f.id = s.codigo_ccm)");

        exec("insert into " + E + ".LOGRADOUROS_CEPS (ID, CIDADE, DISTRITO, LOGRADOURO,"
                + " CD_CCM_LOGRADOURO, CD_CCM_CEP, IDENTIFICACAO, BANCO)"
                + " select " + E + ".SEQ_LOGRADOUROS_CEPS.nextval, s.cidade, s.distrito, s.logradouro,"
                + " s.lid, s.codigo_ccm, null, null"
                + " from (" + origem + ") s"
                + " where exists (select 1 from " + E + ".CEPS f where f.id = s.codigo_ccm)"
                + " and not exists (select 1 from " + E + ".LOGRADOUROS_CEPS f where f.cd_ccm_cep = s.codigo_ccm)");

        exec("insert into " + E + ".BAIRROS_CEPS (ID, CIDADE, DISTRITO, BAIRRO,"
                + " CD_CCM_BAIRRO, CD_CCM_CEP, IDENTIFICACAO, BANCO)"
                + " select " + E + ".SEQ_BAIRROS_CEPS.nextval, s.cidade, s.distrito, s.bairro,"
                + " s.bid, s.codigo_ccm, null, null"
                + " from (" + origem + ") s"
                + " where exists (select 1 from " + E + ".CEPS f where f.id = s.codigo_ccm)"
                + " and not exists (select 1 from " + E + ".BAIRROS_CEPS f where f.cd_ccm_cep = s.codigo_ccm)");

        exec("insert into " + M + ".LOGRADOUROS_CEPS (ID, CIDADE, DISTRITO, LOGRADOURO,"
                + " CD_CCM_LOGRADOURO, CD_CCM_CEP)"
                + " select " + E + ".SEQ_LOGRADOUROS_CEPS.nextval, s.cidade, s.distrito, s.logradouro,"
                + " s.lid, s.codigo_ccm"
                + " from (" + origem + ") s"
                + " where exists (select 1 from " + M + ".CEPS f where f.id = s.codigo_ccm)"
                + " and not exists (select 1 from " + M + ".LOGRADOUROS_CEPS f where f.cd_ccm_cep = s.codigo_ccm)");

        exec("insert into " + M + ".BAIRROS_CEPS (ID, CIDADE, DISTRITO, BAIRRO, LOGRADOURO,"
                + " CD_CCM_BAIRRO, CD_CCM_CEP)"
                + " select " + E + ".SEQ_BAIRROS_CEPS.nextval, s.cidade, s.distrito, s.bairro, s.logradouro,"
                + " s.bid, s.codigo_ccm"
                + " from (" + origem + ") s"
                + " where exists (select 1 from " + M + ".CEPS f where f.id = s.codigo_ccm)"
                + " and not exists (select 1 from " + M + ".BAIRROS_CEPS f where f.cd_ccm_cep = s.codigo_ccm)");

        return new long[]{novos, reaproveitados, e, m};
    }

    /**********************************************************************
     * UTILITÁRIOS
     **********************************************************************/

    /*
     * O distrito da linha (cidade + distrito) já tem CODIGO_CCM e existe em
     * DBO_CC_ENDERECOS.DISTRITOS.
     */
    private String existeDistritoFinal(String alias) {
        return "exists (select 1 from " + U + ".DISTRITOS d"
                + " join " + E + ".DISTRITOS fd on fd.id = d.codigo_ccm"
                + " where d.cidade = " + alias + ".cidade and d.distrito = " + alias + ".distrito)";
    }

    private long[] contarComCodigo(
            String tabela,
            String condicaoPronto,
            String alias,
            String finalE,
            String finalM) {

        String base = " from " + tabela + " " + alias;

        long total = count("select count(*)" + base);
        long com = count("select count(*)" + base + " where " + alias + ".codigo_ccm > 0");
        long pend = count("select count(*)" + base + " where nvl(" + alias + ".codigo_ccm, 0) = 0 and "
                + condicaoPronto);

        String codigos = "(select distinct codigo_ccm from " + tabela + " where codigo_ccm > 0) c";

        return new long[]{
                total,
                com,
                pend,
                Math.max(total - com - pend, 0L),
                count("select count(*) from " + codigos
                        + " where not exists (select 1 from " + finalE + " f where f.id = c.codigo_ccm)"),
                count("select count(*) from " + codigos
                        + " where not exists (select 1 from " + finalM + " f where f.id = c.codigo_ccm)")
        };
    }

    private long exec(String sql) {
        return manager.createNativeQuery(sql).executeUpdate();
    }

    private long count(String sql) {
        Object result = manager.createNativeQuery(sql).getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }
}
