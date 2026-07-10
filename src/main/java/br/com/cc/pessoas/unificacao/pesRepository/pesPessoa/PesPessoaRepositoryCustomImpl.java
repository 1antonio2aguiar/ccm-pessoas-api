package br.com.cc.pessoas.unificacao.pesRepository.pesPessoa;

import br.com.cc.pessoas.unificacao.pesDto.PesPessoaDTO;
import br.com.cc.pessoas.unificacao.pesFilter.PesPessoaFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class PesPessoaRepositoryCustomImpl implements PesPessoaRepositoryCustom {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<PesPessoaDTO> filtrarCpfUnicoNaoMigradas(PesPessoaFilter filter, Pageable pageable) {

        StringBuilder fromWhere = new StringBuilder();
        fromWhere.append("""
            from dbo_ccm_pessoas.pes_pessoas p
            where p.fisica_juridica = 'F'
              and p.cgc_cpf is not null
              and p.cgc_cpf != 0
              and p.cgc_cpf not in (19100, 29200, 39300, 49400)
              and not exists (
                    select 1
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                     where cup.cd_origem = p.pessoa
              )
              and not exists (
                    select 1
                      from dbo_ccm_pessoas.pes_pessoas p2
                     where p2.fisica_juridica = 'F'
                       and p2.cgc_cpf = p.cgc_cpf
                       and p2.pessoa <> p.pessoa
              )
        """);

        if (filter.getPessoa() != null) {
            fromWhere.append(" and p.pessoa = :pessoa ");
        }

        if (StringUtils.hasText(filter.getNome())) {
            fromWhere.append(" and upper(p.nome) like :nome ");
        }

        if (filter.getCpf() != null) {
            fromWhere.append(" and p.cgc_cpf = :cpf ");
        }

        if (filter.getDataNascimento() != null) {
            fromWhere.append(" and p.data_nascimento >= :dataNascimentoIni ");
            fromWhere.append(" and p.data_nascimento < :dataNascimentoFim ");
        }

        String orderBy = " order by p.nome ";

        String sqlIds = "select p.pessoa " + fromWhere + orderBy;
        String sqlCount = "select count(*) " + fromWhere;

        Query queryIds = manager.createNativeQuery(sqlIds);
        Query queryCount = manager.createNativeQuery(sqlCount);

        aplicarParametros(filter, queryIds);
        aplicarParametros(filter, queryCount);

        queryIds.setFirstResult((int) pageable.getOffset());
        queryIds.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Number> idsRaw = queryIds.getResultList();

        Long total = ((Number) queryCount.getSingleResult()).longValue();

        if (idsRaw == null || idsRaw.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, total);
        }

        List<Long> ids = idsRaw.stream().map(Number::longValue).toList();

        String sqlDados = """
            select
                p.pessoa,
                p.nome,
                p.fisica_juridica,
                p.data_cadastro,
                p.cgc_cpf,
                p.tipo_pessoa,
                tp.descricao as tipo_pessoa_descricao,
                p.cidade,
                c.nome as cidade_nome,
                c.estado as uf,
                p.distrito,
                d.nome as distrito_nome,
                p.bairro,
                b.nome as bairro_nome,
                p.logradouro,
                l.nome as logradouro_nome,
                l.tipo_logradouro as tipo_logradouro,
                p.numero,
                p.complemento,
                p.cep,
                p.data_nascimento,
                p.estado_civil,
                p.sexo,
                p.cidade_nascimento,
                cast(null as varchar2(200)) as cidade_nascimento_nome,
                p.pais,
                p.tipo_documento,
                td.descricao as tipo_documento_descricao,
                p.numero_docto,
                p.orgao_docto,
                p.emissao_docto,
                p.titulo_eleitoral,
                p.zona,
                p.secao,
                p.mae,
                p.pai,
                p.telefone,
                p.recado,
                p.celular,
                p.fax,
                p.e_mail,
                p.pagina_web,
                p.pessoa_matriz,
                p.inscricao_estadual,
                p.fantasia,
                p.profissao,
                p.vip,
                p.observacao,
                p.conjugue,
                p.objeto_social,
                p.microempresa,
                p.mes_envio_sicom,
                p.ano_envio_sicom,
                p.tipo_empresa,
                p.nome_social,
                p.deficiente
            from dbo_ccm_pessoas.pes_pessoas p
            left join dbo_ccm_pessoas.pes_tipos_pessoas tp
                   on tp.tipo_pessoa = p.tipo_pessoa
            left join dbo_ccm_pessoas.pes_cidades c
                   on c.cidade = p.cidade
            left join dbo_ccm_pessoas.pes_distritos d
                   on d.cidade = p.cidade
                  and d.distrito = p.distrito
            left join dbo_ccm_pessoas.pes_bairros b
                   on b.cidade = p.cidade
                  and b.distrito = p.distrito
                  and b.bairro = p.bairro
            left join dbo_ccm_pessoas.pes_logradouros l
                   on l.cidade = p.cidade
                  and l.distrito = p.distrito
                  and l.logradouro = p.logradouro
            left join dbo_ccm_pessoas.pes_tipos_documentos td
                   on td.tipo_documento = p.tipo_documento
            where p.pessoa in (:ids)
            order by p.nome
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = manager.createNativeQuery(sqlDados)
                .setParameter("ids", ids)
                .getResultList();

        List<PesPessoaDTO> pessoas = rows.stream().map(this::toDto).toList();

        return new PageImpl<>(pessoas, pageable, total);
    }

    public Page<PesPessoaDTO> filtrarCpfDuplicadoNaoMigradas(PesPessoaFilter filter, Pageable pageable) {

        StringBuilder fromWhere = new StringBuilder();
        fromWhere.append("""
            from dbo_ccm_pessoas.pes_pessoas p
            where p.fisica_juridica = 'F'
              and p.cgc_cpf is not null
              and p.cgc_cpf != 0
              and p.cgc_cpf > 100000000
              and not exists (
                    select 1
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                     where cup.cd_origem = p.pessoa
              )
              and exists (
                    select 1
                      from dbo_ccm_pessoas.pes_pessoas p2
                     where p2.fisica_juridica = 'F'
                       and p2.cgc_cpf = p.cgc_cpf
                       and upper(trim(p2.nome)) = upper(trim(p.nome))
                       and p2.pessoa <> p.pessoa
              )
        """);

        if (filter.getPessoa() != null) {
            fromWhere.append(" and p.pessoa = :pessoa ");
        }

        if (StringUtils.hasText(filter.getNome())) {
            fromWhere.append(" and upper(p.nome) like :nome ");
        }

        if (filter.getCpf() != null) {
            fromWhere.append(" and p.cgc_cpf = :cpf ");
        }

        if (filter.getDataNascimento() != null) {
            fromWhere.append(" and p.data_nascimento >= :dataNascimentoIni ");
            fromWhere.append(" and p.data_nascimento < :dataNascimentoFim ");
        }

        String orderBy = " order by p.cgc_cpf, upper(trim(p.nome)), p.pessoa ";

        String sqlIds = "select p.pessoa " + fromWhere + orderBy;
        String sqlCount = "select count(*) " + fromWhere;

        Query queryIds = manager.createNativeQuery(sqlIds);
        Query queryCount = manager.createNativeQuery(sqlCount);

        aplicarParametros(filter, queryIds);
        aplicarParametros(filter, queryCount);

        queryIds.setFirstResult((int) pageable.getOffset());
        queryIds.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object> idsRaw = queryIds.getResultList();

        Long total = ((Number) queryCount.getSingleResult()).longValue();

        if (idsRaw == null || idsRaw.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, total);
        }

        List<Long> ids = idsRaw.stream()
                .map(this::num)
                .filter(Objects::nonNull)
                .toList();

        String sqlDados = """
            select
                p.pessoa,
                p.nome,
                p.fisica_juridica,
                p.data_cadastro,
                p.cgc_cpf,
                p.tipo_pessoa,
                tp.descricao as tipo_pessoa_descricao,
                p.cidade,
                c.nome as cidade_nome,
                c.estado as uf,
                p.distrito,
                d.nome as distrito_nome,
                p.bairro,
                b.nome as bairro_nome,
                p.logradouro,
                l.nome as logradouro_nome,
                l.tipo_logradouro as tipo_logradouro,
                p.numero,
                p.complemento,
                p.cep,
                p.data_nascimento,
                p.estado_civil,
                p.sexo,
                p.cidade_nascimento,
                cast(null as varchar2(200)) as cidade_nascimento_nome,
                p.pais,
                p.tipo_documento,
                td.descricao as tipo_documento_descricao,
                p.numero_docto,
                p.orgao_docto,
                p.emissao_docto,
                p.titulo_eleitoral,
                p.zona,
                p.secao,
                p.mae,
                p.pai,
                p.telefone,
                p.recado,
                p.celular,
                p.fax,
                p.e_mail,
                p.pagina_web,
                p.pessoa_matriz,
                p.inscricao_estadual,
                p.fantasia,
                p.profissao,
                p.vip,
                p.observacao,
                p.conjugue,
                p.objeto_social,
                p.microempresa,
                p.mes_envio_sicom,
                p.ano_envio_sicom,
                p.tipo_empresa,
                p.nome_social,
                p.deficiente
            from dbo_ccm_pessoas.pes_pessoas p
            left join dbo_ccm_pessoas.pes_tipos_pessoas tp
                   on tp.tipo_pessoa = p.tipo_pessoa
            left join dbo_ccm_pessoas.pes_cidades c
                   on c.cidade = p.cidade
            left join dbo_ccm_pessoas.pes_distritos d
                   on d.cidade = p.cidade
                  and d.distrito = p.distrito
            left join dbo_ccm_pessoas.pes_bairros b
                   on b.cidade = p.cidade
                  and b.distrito = p.distrito
                  and b.bairro = p.bairro
            left join dbo_ccm_pessoas.pes_logradouros l
                   on l.cidade = p.cidade
                  and l.distrito = p.distrito
                  and l.logradouro = p.logradouro
            left join dbo_ccm_pessoas.pes_tipos_documentos td
                   on td.tipo_documento = p.tipo_documento
            where p.pessoa in (:ids)
            order by p.cgc_cpf, upper(trim(p.nome)), p.pessoa
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = manager.createNativeQuery(sqlDados)
                .setParameter("ids", ids)
                .getResultList();

        List<PesPessoaDTO> pessoas = rows.stream().map(this::toDto).toList();

        return new PageImpl<>(pessoas, pageable, total);
    }

    private void aplicarParametros(PesPessoaFilter filter, Query query) {
        if (filter.getPessoa() != null) {
            query.setParameter("pessoa", filter.getPessoa());
        }

        if (StringUtils.hasText(filter.getNome())) {
            query.setParameter("nome", "%" + filter.getNome().trim().toUpperCase() + "%");
        }

        if (filter.getCpf() != null) {
            query.setParameter("cpf", filter.getCpf());
        }

        if (filter.getDataNascimento() != null) {
            LocalDateTime ini = filter.getDataNascimento().atStartOfDay();
            LocalDateTime fim = filter.getDataNascimento().plusDays(1).atStartOfDay();

            query.setParameter("dataNascimentoIni", Timestamp.valueOf(ini));
            query.setParameter("dataNascimentoFim", Timestamp.valueOf(fim));
        }
    }

    private PesPessoaDTO toDto(Object[] r) {
        return new PesPessoaDTO(
                num(r[0]),
                str(r[1]),
                str(r[2]),
                ldt(r[3]),
                num(r[4]),

                num(r[5]),
                str(r[6]),

                num(r[7]),
                str(r[8]),
                str(r[9]),

                num(r[10]),
                str(r[11]),

                num(r[12]),
                str(r[13]),

                num(r[14]),
                str(r[15]),
                str(r[16]),

                num(r[17]),
                str(r[18]),
                num(r[19]),

                ldt(r[20]),
                str(r[21]),
                str(r[22]),

                num(r[23]),
                str(r[24]),

                num(r[25]),

                num(r[26]),
                str(r[27]),

                str(r[28]),
                str(r[29]),
                ldt(r[30]),

                num(r[31]),
                num(r[32]),
                num(r[33]),

                str(r[34]),
                str(r[35]),

                num(r[36]),
                num(r[37]),
                num(r[38]),
                num(r[39]),
                str(r[40]),
                str(r[41]),

                num(r[42]),
                str(r[43]),
                str(r[44]),
                num(r[45]),
                str(r[46]),
                str(r[47]),
                str(r[48]),
                str(r[49]),
                str(r[50]),
                num(r[51]),
                num(r[52]),
                num(r[53]),
                str(r[54]),
                str(r[55])
        );
    }

    private Long num(Object o) {
        if (o == null) {
            return null;
        }

        if (o instanceof Number n) {
            return n.longValue();
        }

        String s = String.valueOf(o).trim();

        if (s.isEmpty()) {
            return null;
        }

        return Long.valueOf(s);
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private LocalDateTime ldt(Object o) {
        if (o == null) return null;
        if (o instanceof Timestamp ts) return ts.toLocalDateTime();
        return null;
    }

    private Long numSafe(Object o) {
        if (o == null) return null;

        if (o instanceof Number n) {
            return n.longValue();
        }

        String s = String.valueOf(o).trim();

        if (s.isEmpty()) return null;

        // 👉 se NÃO for número, ignora
        if (!s.matches("\\d+")) {
            return null;
        }

        return Long.valueOf(s);
    }

    /*  CNPJ UNICO */
    @Override
    public Page<PesPessoaDTO> filtrarCnpjUnicoNaoMigradas(PesPessoaFilter filter, Pageable pageable) {

        StringBuilder fromWhere = new StringBuilder();
        fromWhere.append("""
        from dbo_ccm_pessoas.pes_pessoas p
        where p.fisica_juridica = 'J'
          and p.cgc_cpf is not null
          and p.cgc_cpf != 0
          and not exists (
                select 1
                  from dbo_ccm_pessoas.cad_unico_pessoa cup
                 where cup.cd_origem = p.pessoa
          )
          and not exists (
                select 1
                  from dbo_ccm_pessoas.pes_pessoas p2
                 where p2.fisica_juridica = 'J'
                   and p2.cgc_cpf = p.cgc_cpf
                   and p2.pessoa <> p.pessoa
          )
    """);

        if (filter.getPessoa() != null) {
            fromWhere.append(" and p.pessoa = :pessoa ");
        }

        if (StringUtils.hasText(filter.getNome())) {
            fromWhere.append(" and upper(p.nome) like :nome ");
        }

        if (filter.getCnpj() != null) {
            fromWhere.append(" and p.cgc_cpf = :cnpj ");
        }

        String orderBy = " order by p.nome ";

        String sqlIds = "select p.pessoa " + fromWhere + orderBy;
        String sqlCount = "select count(*) " + fromWhere;

        Query queryIds = manager.createNativeQuery(sqlIds);
        Query queryCount = manager.createNativeQuery(sqlCount);

        aplicarParametrosCnpj(filter, queryIds);
        aplicarParametrosCnpj(filter, queryCount);

        queryIds.setFirstResult((int) pageable.getOffset());
        queryIds.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Number> idsRaw = queryIds.getResultList();

        Long total = ((Number) queryCount.getSingleResult()).longValue();

        if (idsRaw == null || idsRaw.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, total);
        }

        List<Long> ids = idsRaw.stream().map(Number::longValue).toList();

        String sqlDados = """
        select
            p.pessoa,
            p.nome,
            p.fisica_juridica,
            p.data_cadastro,
            p.cgc_cpf,
            p.tipo_pessoa,
            tp.descricao as tipo_pessoa_descricao,
            p.cidade,
            c.nome as cidade_nome,
            c.estado as uf,
            p.distrito,
            d.nome as distrito_nome,
            p.bairro,
            b.nome as bairro_nome,
            p.logradouro,
            l.nome as logradouro_nome,
            l.tipo_logradouro as tipo_logradouro,
            p.numero,
            p.complemento,
            p.cep,
            p.data_nascimento,
            p.estado_civil,
            p.sexo,
            p.cidade_nascimento,
            cast(null as varchar2(200)) as cidade_nascimento_nome,
            p.pais,
            p.tipo_documento,
            td.descricao as tipo_documento_descricao,
            p.numero_docto,
            p.orgao_docto,
            p.emissao_docto,
            p.titulo_eleitoral,
            p.zona,
            p.secao,
            p.mae,
            p.pai,
            p.telefone,
            p.recado,
            p.celular,
            p.fax,
            p.e_mail,
            p.pagina_web,
            p.pessoa_matriz,
            p.inscricao_estadual,
            p.fantasia,
            p.profissao,
            p.vip,
            p.observacao,
            p.conjugue,
            p.objeto_social,
            p.microempresa,
            p.mes_envio_sicom,
            p.ano_envio_sicom,
            p.tipo_empresa,
            p.nome_social,
            p.deficiente
        from dbo_ccm_pessoas.pes_pessoas p
        left join dbo_ccm_pessoas.pes_tipos_pessoas tp
               on tp.tipo_pessoa = p.tipo_pessoa
        left join dbo_ccm_pessoas.pes_cidades c
               on c.cidade = p.cidade
        left join dbo_ccm_pessoas.pes_distritos d
               on d.cidade = p.cidade
              and d.distrito = p.distrito
        left join dbo_ccm_pessoas.pes_bairros b
               on b.cidade = p.cidade
              and b.distrito = p.distrito
              and b.bairro = p.bairro
        left join dbo_ccm_pessoas.pes_logradouros l
               on l.cidade = p.cidade
              and l.distrito = p.distrito
              and l.logradouro = p.logradouro
        left join dbo_ccm_pessoas.pes_tipos_documentos td
               on td.tipo_documento = p.tipo_documento
        where p.pessoa in (:ids)
        order by p.nome
    """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = manager.createNativeQuery(sqlDados)
                .setParameter("ids", ids)
                .getResultList();

        List<PesPessoaDTO> pessoas = rows.stream().map(this::toDto).toList();

        return new PageImpl<>(pessoas, pageable, total);
    }

    /* CNPJ DUPLICADO */
    @Override
    public Page<PesPessoaDTO> filtrarCnpjDuplicadoNaoMigradas(PesPessoaFilter filter, Pageable pageable) {

        StringBuilder fromWhere = new StringBuilder();
        fromWhere.append("""
            from dbo_ccm_pessoas.pes_pessoas p
            where p.fisica_juridica = 'J'
              and p.cgc_cpf is not null
              and p.cgc_cpf != 0
              and p.cgc_cpf NOT IN (191,272)
              and not exists (
                    select 1
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                     where cup.cd_origem = p.pessoa
              )
              and exists (
                    select 1
                      from dbo_ccm_pessoas.pes_pessoas p2
                     where p2.fisica_juridica = 'J'
                       and p2.cgc_cpf = p.cgc_cpf
                       and upper(trim(p2.nome)) = upper(trim(p.nome))
                       and p2.pessoa <> p.pessoa
              )
        """);

        if (filter.getPessoa() != null) {
            fromWhere.append(" and p.pessoa = :pessoa ");
        }

        if (StringUtils.hasText(filter.getNome())) {
            fromWhere.append(" and upper(p.nome) like :nome ");
        }

        if (filter.getCnpj() != null) {
            fromWhere.append(" and p.cgc_cpf = :cnpj ");
        }

        String orderBy = " order by p.cgc_cpf, upper(trim(p.nome)), p.pessoa ";

        String sqlIds = "select p.pessoa " + fromWhere + orderBy;
        String sqlCount = "select count(*) " + fromWhere;

        Query queryIds = manager.createNativeQuery(sqlIds);
        Query queryCount = manager.createNativeQuery(sqlCount);

        aplicarParametrosCnpj(filter, queryIds);
        aplicarParametrosCnpj(filter, queryCount);

        queryIds.setFirstResult((int) pageable.getOffset());
        queryIds.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object> idsRaw = queryIds.getResultList();

        Long total = ((Number) queryCount.getSingleResult()).longValue();

        if (idsRaw == null || idsRaw.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, total);
        }

        List<Long> ids = idsRaw.stream()
                .map(this::num)
                .filter(Objects::nonNull)
                .toList();

        String sqlDados = """
            select
                p.pessoa,
                p.nome,
                p.fisica_juridica,
                p.data_cadastro,
                p.cgc_cpf,
                p.tipo_pessoa,
                tp.descricao as tipo_pessoa_descricao,
                p.cidade,
                c.nome as cidade_nome,
                c.estado as uf,
                p.distrito,
                d.nome as distrito_nome,
                p.bairro,
                b.nome as bairro_nome,
                p.logradouro,
                l.nome as logradouro_nome,
                l.tipo_logradouro as tipo_logradouro,
                p.numero,
                p.complemento,
                p.cep,
                p.data_nascimento,
                p.estado_civil,
                p.sexo,
                p.cidade_nascimento,
                cast(null as varchar2(200)) as cidade_nascimento_nome,
                p.pais,
                p.tipo_documento,
                td.descricao as tipo_documento_descricao,
                p.numero_docto,
                p.orgao_docto,
                p.emissao_docto,
                p.titulo_eleitoral,
                p.zona,
                p.secao,
                p.mae,
                p.pai,
                p.telefone,
                p.recado,
                p.celular,
                p.fax,
                p.e_mail,
                p.pagina_web,
                p.pessoa_matriz,
                p.inscricao_estadual,
                p.fantasia,
                p.profissao,
                p.vip,
                p.observacao,
                p.conjugue,
                p.objeto_social,
                p.microempresa,
                p.mes_envio_sicom,
                p.ano_envio_sicom,
                p.tipo_empresa,
                p.nome_social,
                p.deficiente
            from dbo_ccm_pessoas.pes_pessoas p
            left join dbo_ccm_pessoas.pes_tipos_pessoas tp
                   on tp.tipo_pessoa = p.tipo_pessoa
            left join dbo_ccm_pessoas.pes_cidades c
                   on c.cidade = p.cidade
            left join dbo_ccm_pessoas.pes_distritos d
                   on d.cidade = p.cidade
                  and d.distrito = p.distrito
            left join dbo_ccm_pessoas.pes_bairros b
                   on b.cidade = p.cidade
                  and b.distrito = p.distrito
                  and b.bairro = p.bairro
            left join dbo_ccm_pessoas.pes_logradouros l
                   on l.cidade = p.cidade
                  and l.distrito = p.distrito
                  and l.logradouro = p.logradouro
            left join dbo_ccm_pessoas.pes_tipos_documentos td
                   on td.tipo_documento = p.tipo_documento
            where p.pessoa in (:ids)
            order by p.cgc_cpf, upper(trim(p.nome)), p.pessoa
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = manager.createNativeQuery(sqlDados)
                .setParameter("ids", ids)
                .getResultList();

        List<PesPessoaDTO> pessoas = rows.stream().map(this::toDto).toList();

        return new PageImpl<>(pessoas, pageable, total);
    }

    private void aplicarParametrosCnpj(PesPessoaFilter filter, Query query) {
        if (filter.getPessoa() != null) {
            query.setParameter("pessoa", filter.getPessoa());
        }

        if (StringUtils.hasText(filter.getNome())) {
            query.setParameter("nome", "%" + filter.getNome().trim().toUpperCase() + "%");
        }

        if (filter.getCnpj() != null) {
            query.setParameter("cnpj", filter.getCnpj());
        }
    }
}