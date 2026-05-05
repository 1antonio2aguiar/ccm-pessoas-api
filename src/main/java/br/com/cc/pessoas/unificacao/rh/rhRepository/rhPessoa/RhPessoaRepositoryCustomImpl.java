package br.com.cc.pessoas.unificacao.rh.rhRepository.rhPessoa;

import br.com.cc.pessoas.unificacao.rh.rhDto.RhPessoaDTO;
import br.com.cc.pessoas.unificacao.rh.rhFilter.RhPessoaFilter;
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

@Repository
public class RhPessoaRepositoryCustomImpl implements RhPessoaRepositoryCustom {

    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<RhPessoaDTO> filtrarListaRh(RhPessoaFilter filter, Pageable pageable) {

        StringBuilder fromWhere = new StringBuilder();

        fromWhere.append("""
            from dbo_rh.pessoas p
            where 1 = 1
              and not exists (
                    select 1
                      from dbo_ccm_pessoas.cad_unico_pessoa cup_rh
                     where cup_rh.cd_origem = p.pessoa
                       and cup_rh.banco = 'RH'
              )
        """);

        if (StringUtils.hasText(filter.getFisicaJuridica())) {
            fromWhere.append(" and p.fisica_juridica = :fisicaJuridica ");
        }

        if (filter.getPessoa() != null) {
            fromWhere.append(" and p.pessoa = :pessoa ");
        }

        if (StringUtils.hasText(filter.getNome())) {
            fromWhere.append(" and upper(p.nome) like :nome ");
        }

        if (filter.getCpf() != null) {
            fromWhere.append(" and p.cgc_cpf = :cpf ");
        }

        if (filter.getCnpj() != null) {
            fromWhere.append(" and p.cgc_cpf = :cnpj ");
        }

        if (filter.getDataNascimento() != null) {
            fromWhere.append(" and p.data_nascimento >= :dataNascimentoIni ");
            fromWhere.append(" and p.data_nascimento < :dataNascimentoFim ");
        }

        if (StringUtils.hasText(filter.getStatusCadastro())) {

            String status = filter.getStatusCadastro().trim().toUpperCase();

            if ("SEM_CPF_CNPJ".equals(status)) {
                fromWhere.append("""
                    and (p.cgc_cpf is null or p.cgc_cpf = 0)
                """);
            }

            if ("JA_EXISTE_CAD_UNICO".equals(status)) {
                fromWhere.append("""
                    and p.cgc_cpf is not null
                    and p.cgc_cpf <> 0
                    and exists (
                        select 1
                          from dbo_ccm_pessoas.cad_unico_pessoa cup
                         where cup.cpf_cnpj = p.cgc_cpf
                           and cup.banco = 'PESSOAS'
                           and replace(transf_caracte(cup.nome), ' ', '') =
                               replace(transf_caracte(p.nome), ' ', '')
                    )
                """);
            }

            if ("DUPLICADO_RH".equals(status)) {
                fromWhere.append("""
                    and p.cgc_cpf is not null
                    and p.cgc_cpf <> 0
                    and not exists (
                        select 1
                          from dbo_ccm_pessoas.cad_unico_pessoa cup
                         where cup.cpf_cnpj = p.cgc_cpf
                           and cup.banco = 'PESSOAS'
                           and replace(transf_caracte(cup.nome), ' ', '') =
                               replace(transf_caracte(p.nome), ' ', '')
                    )
                    and exists (
                        select 1
                          from dbo_rh.pessoas p2
                         where p2.cgc_cpf = p.cgc_cpf
                           and replace(transf_caracte(p2.nome), ' ', '') =
                               replace(transf_caracte(p.nome), ' ', '')
                           and p2.pessoa <> p.pessoa
                    )
                """);
            }

            if ("UNICO_RH".equals(status)) {
                fromWhere.append("""
                    and p.cgc_cpf is not null
                    and p.cgc_cpf <> 0
                    and not exists (
                        select 1
                          from dbo_ccm_pessoas.cad_unico_pessoa cup
                         where cup.cpf_cnpj = p.cgc_cpf
                           and cup.banco = 'PESSOAS'
                           and replace(transf_caracte(cup.nome), ' ', '') =
                               replace(transf_caracte(p.nome), ' ', '')
                    )
                    and not exists (
                        select 1
                          from dbo_rh.pessoas p2
                         where p2.cgc_cpf = p.cgc_cpf
                           and replace(transf_caracte(p2.nome), ' ', '') =
                               replace(transf_caracte(p.nome), ' ', '')
                           and p2.pessoa <> p.pessoa
                    )
                """);
            }
        }

        String orderBy = " order by p.nome, p.cgc_cpf, p.pessoa ";

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

                p.ddd_telefone,
                p.telefone,
                p.ddd_recado,
                p.recado,
                p.ddd_celular,
                p.celular,
                p.whatsapp,
                p.fax,

                p.e_mail,
                p.pagina_web,

                p.pessoa_matriz,
                p.inscricao_estadual,
                p.fantasia,
                p.profissao,
                p.vip,
                p.nome_conjuge,
                p.mes_envio_sicom,
                p.nome_social,
                p.instagram,
                p.facebook,

                case
                    when p.cgc_cpf is null or p.cgc_cpf = 0 then 'SEM_CPF_CNPJ'

                    when exists (
                        select 1
                          from dbo_ccm_pessoas.cad_unico_pessoa cup
                         where cup.cpf_cnpj = p.cgc_cpf
                           and cup.banco = 'PESSOAS'
                           and replace(transf_caracte(cup.nome), ' ', '') =
                               replace(transf_caracte(p.nome), ' ', '')
                    ) then 'JA_EXISTE_CAD_UNICO'

                    when exists (
                        select 1
                          from dbo_rh.pessoas p2
                         where p2.cgc_cpf = p.cgc_cpf
                           and replace(transf_caracte(p2.nome), ' ', '') =
                               replace(transf_caracte(p.nome), ' ', '')
                           and p2.pessoa <> p.pessoa
                    ) then 'DUPLICADO_RH'

                    else 'UNICO_RH'
                end as status_cadastro

            from dbo_rh.pessoas p

            left join dbo_rh.tipos_pessoas tp
                   on tp.tipo_pessoa = p.tipo_pessoa

            left join dbo_rh.cidades c
                   on c.cidade = p.cidade

            left join dbo_rh.distritos d
                   on d.cidade = p.cidade
                  and d.distrito = p.distrito

            left join dbo_rh.bairros b
                   on b.cidade = p.cidade
                  and b.distrito = p.distrito
                  and b.bairro = p.bairro

            left join dbo_rh.logradouros l
                   on l.cidade = p.cidade
                  and l.distrito = p.distrito
                  and l.logradouro = p.logradouro

            left join dbo_rh.tipos_documentos td
                   on td.tipo_documento = p.tipo_documento

            where p.pessoa in (:ids)

            order by p.nome, p.cgc_cpf, p.pessoa
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = manager.createNativeQuery(sqlDados)
                .setParameter("ids", ids)
                .getResultList();

        List<RhPessoaDTO> pessoas = rows.stream().map(this::toDto).toList();

        return new PageImpl<>(pessoas, pageable, total);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Long> buscarGrupoDuplicadoCpfRh(Long pessoaId) {
        if (pessoaId == null) {
            return new ArrayList<>();
        }

        List<Number> ids = manager.createNativeQuery("""
        select p.pessoa
          from dbo_rh.pessoas p
         where p.fisica_juridica = 'F'
           and p.cgc_cpf = (
                select p0.cgc_cpf
                  from dbo_rh.pessoas p0
                 where p0.pessoa = :pessoaId
           )
           and replace(transf_caracte(p.nome), ' ', '') = (
                select replace(transf_caracte(p1.nome), ' ', '')
                  from dbo_rh.pessoas p1
                 where p1.pessoa = :pessoaId
           )
           and not exists (
                select 1
                  from dbo_ccm_pessoas.cad_unico_pessoa cup
                 where cup.cd_origem = p.pessoa
                   and cup.banco = 'RH'
           )
         order by p.nome, p.cgc_cpf, p.pessoa
    """)
                .setParameter("pessoaId", pessoaId)
                .getResultList();

        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        return ids.stream()
                .map(Number::longValue)
                .toList();
    }

    private void aplicarParametros(RhPessoaFilter filter, Query query) {
        if (filter.getPessoa() != null) {
            query.setParameter("pessoa", filter.getPessoa());
        }

        if (StringUtils.hasText(filter.getNome())) {
            query.setParameter("nome", "%" + filter.getNome().trim().toUpperCase() + "%");
        }

        if (filter.getCpf() != null) {
            query.setParameter("cpf", filter.getCpf());
        }

        if (filter.getCnpj() != null) {
            query.setParameter("cnpj", filter.getCnpj());
        }

        if (filter.getDataNascimento() != null) {
            LocalDateTime ini = filter.getDataNascimento().atStartOfDay();
            LocalDateTime fim = filter.getDataNascimento().plusDays(1).atStartOfDay();

            query.setParameter("dataNascimentoIni", Timestamp.valueOf(ini));
            query.setParameter("dataNascimentoFim", Timestamp.valueOf(fim));
        }

        if (StringUtils.hasText(filter.getFisicaJuridica())) {
            query.setParameter("fisicaJuridica", filter.getFisicaJuridica().trim().toUpperCase());
        }
    }

    private RhPessoaDTO toDto(Object[] r) {
        return new RhPessoaDTO(
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
                num(r[40]),
                num(r[41]),
                num(r[42]),
                num(r[43]),

                str(r[44]),
                str(r[45]),

                num(r[46]),
                str(r[47]),
                str(r[48]),
                num(r[49]),

                str(r[50]),
                str(r[51]),
                num(r[52]),
                str(r[53]),
                str(r[54]),
                str(r[55]),
                str(r[56])
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
}