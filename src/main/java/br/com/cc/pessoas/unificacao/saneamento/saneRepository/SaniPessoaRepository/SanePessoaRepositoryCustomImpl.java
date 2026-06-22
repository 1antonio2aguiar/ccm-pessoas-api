package br.com.cc.pessoas.unificacao.saneamento.saneRepository.SaniPessoaRepository;

import br.com.cc.pessoas.unificacao.saneamento.saneDto.SanePessoaDTO;
import br.com.cc.pessoas.unificacao.saneamento.saneFilter.SanePessoaFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SanePessoaRepositoryCustomImpl implements SanePessoaRepositoryCustom {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<SanePessoaDTO> filtrarListaSaneCpfUnico(SanePessoaFilter filter, Pageable pageable) {

        StringBuilder filtros = new StringBuilder();

        if (filter.getPessoa() != null) {
            filtros.append(" and p.pessoa = :pessoa ");
        }

        if (StringUtils.hasText(filter.getNome())) {
            filtros.append(" and upper(p.nome) like :nome ");
        }

        if (filter.getCpf() != null) {
            filtros.append(" and p.cgc_cpf = :cpf ");
        }

        if (filter.getDataNascimento() != null) {
            filtros.append(" and p.data_nascimento >= :dataNascimentoIni ");
            filtros.append(" and p.data_nascimento < :dataNascimentoFim ");
        }

        int inicio = (int) pageable.getOffset() + 1;
        int fim = inicio + pageable.getPageSize();

        String sqlIds = """
            select pessoa
              from (
                    select x.pessoa,
                           row_number() over(order by x.nome, x.cgc_cpf, x.pessoa) rn
                      from (
                            select p.pessoa, p.nome, p.cgc_cpf
                              from dbo_ccm_pessoas.sane_pessoas p
                             where p.fisica_juridica = 'F'
                               and p.cgc_cpf is not null
                               and p.cgc_cpf <> 0
                               %s

                               and not exists (
                                    select 1
                                      from dbo_ccm_pessoas.cad_unico_pessoa cup_sane
                                     where cup_sane.cd_origem = p.pessoa
                                       and cup_sane.banco = 'SANE'
                               )

                               and not exists (
                                    select 1
                                      from dbo_ccm_pessoas.sane_pessoas p2
                                     where p2.fisica_juridica = 'F'
                                       and p2.cgc_cpf = p.cgc_cpf
                                       and p2.pessoa <> p.pessoa
                               )
                           ) x
                   )
             where rn between :inicio and :fim
        """.formatted(filtros);

        Query queryIds = manager.createNativeQuery(sqlIds);
        aplicarParametros(filter, queryIds);
        queryIds.setParameter("inicio", inicio);
        queryIds.setParameter("fim", fim);

        @SuppressWarnings("unchecked")
        List<Number> idsRaw = queryIds.getResultList();

        if (idsRaw == null || idsRaw.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        boolean hasNext = idsRaw.size() > pageable.getPageSize();

        if (hasNext) {
            idsRaw = idsRaw.subList(0, pageable.getPageSize());
        }

        List<Long> ids = idsRaw.stream()
                .map(Number::longValue)
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

                p.e_mail,
                p.pagina_web,

                p.pessoa_matriz,
                p.inscricao_estadual,
                p.fantasia,
                p.profissao,
                p.vip,

                p.observacao,
                p.aposentado,
                p.inicio_beneficio,
                p.fim_beneficio,
                p.renda_mensal,

                case
                    when exists (
                        select 1
                          from dbo_ccm_pessoas.cad_unico_pessoa cup
                         where cup.cpf_cnpj = p.cgc_cpf
                           and cup.pessoas_cd_unico is not null
                           and (
                                replace(fn_normaliza_texto(cup.nome), ' ', '') =
                                replace(fn_normaliza_texto(p.nome), ' ', '')
                
                                or
                
                                utl_match.edit_distance_similarity(
                                    replace(fn_normaliza_texto(cup.nome), ' ', ''),
                                    replace(fn_normaliza_texto(p.nome), ' ', '')
                                ) >= 80
                           )
                    )
                    then 'EXISTE NO CAD. ÚNICO'
                    else 'ÚNICO'
                end as status_cadastro
            from dbo_ccm_pessoas.sane_pessoas p

            left join dbo_ccm_pessoas.sane_tipos_pessoas tp
                   on tp.tipo_pessoa = p.tipo_pessoa

            left join dbo_ccm_pessoas.sane_cidades c
                   on c.cidade = p.cidade

            left join dbo_ccm_pessoas.sane_distritos d
                   on d.cidade = p.cidade
                  and d.distrito = p.distrito

            left join dbo_ccm_pessoas.sane_bairros b
                   on b.cidade = p.cidade
                  and b.distrito = p.distrito
                  and b.bairro = p.bairro

            left join dbo_ccm_pessoas.sane_logradouros l
                   on l.cidade = p.cidade
                  and l.distrito = p.distrito
                  and l.logradouro = p.logradouro

            left join dbo_ccm_pessoas.sane_tipos_documentos td
                   on td.tipo_documento = p.tipo_documento

            where p.pessoa in (:ids)

            order by p.nome, p.cgc_cpf, p.pessoa
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = manager.createNativeQuery(sqlDados)
                .setParameter("ids", ids)
                .getResultList();

        List<SanePessoaDTO> pessoas = rows.stream()
                .map(this::toDto)
                .toList();

        long totalEstimado = pageable.getOffset() + pessoas.size() + (hasNext ? 1 : 0);

        return new PageImpl<>(pessoas, pageable, totalEstimado);
    }

    @Override
    public Page<SanePessoaDTO> filtrarListaSaneCnpjUnico(SanePessoaFilter filter, Pageable pageable) {

        StringBuilder filtros = new StringBuilder();

        if (filter.getPessoa() != null) {
            filtros.append(" and p.pessoa = :pessoa ");
        }

        if (StringUtils.hasText(filter.getNome())) {
            filtros.append(" and upper(p.nome) like :nome ");
        }

        if (filter.getCnpj() != null) {
            filtros.append(" and p.cgc_cpf = :cnpj ");
        }

        int inicio = (int) pageable.getOffset() + 1;
        int fim = inicio + pageable.getPageSize();

        String sqlIds = """
        select pessoa
          from (
                select x.pessoa,
                       row_number() over(order by x.nome, x.cgc_cpf, x.pessoa) rn
                  from (
                        select p.pessoa, p.nome, p.cgc_cpf
                          from dbo_ccm_pessoas.sane_pessoas p
                         where p.fisica_juridica = 'J'
                           and p.cgc_cpf is not null
                           and p.cgc_cpf <> 0
                           %s

                           and not exists (
                                select 1
                                  from dbo_ccm_pessoas.cad_unico_pessoa cup_sane
                                 where cup_sane.cd_origem = p.pessoa
                                   and cup_sane.banco = 'SANE'
                           )

                           and not exists (
                                select 1
                                  from dbo_ccm_pessoas.sane_pessoas p2
                                 where p2.fisica_juridica = 'J'
                                   and p2.cgc_cpf = p.cgc_cpf
                                   and p2.pessoa <> p.pessoa
                           )
                       ) x
               )
         where rn between :inicio and :fim
    """.formatted(filtros);

        Query queryIds = manager.createNativeQuery(sqlIds);
        aplicarParametrosCnpj(filter, queryIds);
        queryIds.setParameter("inicio", inicio);
        queryIds.setParameter("fim", fim);

        @SuppressWarnings("unchecked")
        List<Number> idsRaw = queryIds.getResultList();

        if (idsRaw == null || idsRaw.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        boolean hasNext = idsRaw.size() > pageable.getPageSize();

        if (hasNext) {
            idsRaw = idsRaw.subList(0, pageable.getPageSize());
        }

        List<Long> ids = idsRaw.stream()
                .map(Number::longValue)
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

            p.e_mail,
            p.pagina_web,

            p.pessoa_matriz,
            p.inscricao_estadual,
            p.fantasia,
            p.profissao,
            p.vip,

            p.observacao,
            p.aposentado,
            p.inicio_beneficio,
            p.fim_beneficio,
            p.renda_mensal,

            case
                when exists (
                    select 1
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                     where cup.cpf_cnpj = p.cgc_cpf
                       and cup.fisica_juridica = 'J'
                       and cup.pessoas_cd_unico is not null
                       and (
                            replace(fn_normaliza_texto(cup.nome), ' ', '') =
                            replace(fn_normaliza_texto(p.nome), ' ', '')

                            or

                            replace(fn_normaliza_texto(cup.nome), ' ', '') like
                            '%' || replace(fn_normaliza_texto(p.nome), ' ', '') || '%'

                            or

                            replace(fn_normaliza_texto(p.nome), ' ', '') like
                            '%' || replace(fn_normaliza_texto(cup.nome), ' ', '') || '%'

                            or

                            utl_match.edit_distance_similarity(
                                replace(fn_normaliza_texto(cup.nome), ' ', ''),
                                replace(fn_normaliza_texto(p.nome), ' ', '')
                            ) >= 80
                       )
                )
                then 'EXISTE NO CAD. ÚNICO'
                else 'ÚNICO'
            end as status_cadastro

        from dbo_ccm_pessoas.sane_pessoas p

        left join dbo_ccm_pessoas.sane_tipos_pessoas tp
               on tp.tipo_pessoa = p.tipo_pessoa

        left join dbo_ccm_pessoas.sane_cidades c
               on c.cidade = p.cidade

        left join dbo_ccm_pessoas.sane_distritos d
               on d.cidade = p.cidade
              and d.distrito = p.distrito

        left join dbo_ccm_pessoas.sane_bairros b
               on b.cidade = p.cidade
              and b.distrito = p.distrito
              and b.bairro = p.bairro

        left join dbo_ccm_pessoas.sane_logradouros l
               on l.cidade = p.cidade
              and l.distrito = p.distrito
              and l.logradouro = p.logradouro

        left join dbo_ccm_pessoas.sane_tipos_documentos td
               on td.tipo_documento = p.tipo_documento

        where p.pessoa in (:ids)

        order by p.nome, p.cgc_cpf, p.pessoa
    """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = manager.createNativeQuery(sqlDados)
                .setParameter("ids", ids)
                .getResultList();

        List<SanePessoaDTO> pessoas = rows.stream()
                .map(this::toDto)
                .toList();

        long totalEstimado = pageable.getOffset() + pessoas.size() + (hasNext ? 1 : 0);

        return new PageImpl<>(pessoas, pageable, totalEstimado);
    }

    private void aplicarParametros(SanePessoaFilter filter, Query query) {
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

    private SanePessoaDTO toDto(Object[] r) {
        return new SanePessoaDTO(
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

                str(r[39]),
                str(r[40]),

                num(r[41]),
                str(r[42]),
                str(r[43]),
                num(r[44]),

                str(r[45]),
                str(r[46]),
                str(r[47]),
                ldt(r[48]),
                ldt(r[49]),
                null,
                str(r[51])
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
        if (o == null) {
            return null;
        }

        if (o instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }

        return null;
    }

    private BigDecimal big(Object o) {
        if (o == null) {
            return null;
        }

        if (o instanceof BigDecimal b) {
            return b;
        }

        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }

        String s = String.valueOf(o).trim();

        if (s.isEmpty()) {
            return null;
        }

        return new BigDecimal(s);
    }
    private void aplicarParametrosCnpj(SanePessoaFilter filter, Query query) {
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