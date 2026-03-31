package br.com.cc.pessoas.unificacao.pesRepository.cadUnicoPessoa;

import br.com.cc.pessoas.unificacao.pesEntity.CadUnicoPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.CadUnicoPessoa_;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa_;
import br.com.cc.pessoas.unificacao.pesFilter.CadUnicoPessoaFilter;
import br.com.cc.pessoas.unificacao.pesFilter.PesPessoaFilter;
import br.com.cc.pessoas.unificacao.pesRepository.cadUnicoPessoa.CadUnicoPessoaRepositoryQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CadUnicoPessoaRepositoryImpl implements CadUnicoPessoaRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<CadUnicoPessoa> filtrar(CadUnicoPessoaFilter cadUnicoPessoa, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<CadUnicoPessoa> criteria = builder.createQuery(CadUnicoPessoa.class);
        Root<CadUnicoPessoa> root = criteria.from(CadUnicoPessoa.class);

        root.fetch("pesCidade", JoinType.LEFT);
        root.fetch("pesDistrito", JoinType.LEFT);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(cadUnicoPessoa, builder, root);
        criteria.where(predicates).orderBy(orders).distinct(true);

        TypedQuery<CadUnicoPessoa> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(cadUnicoPessoa));
    }

    @Override
    public List<CadUnicoPessoa> filtrar(CadUnicoPessoaFilter cadUnicoPessoa) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<CadUnicoPessoa> criteria = builder.createQuery(CadUnicoPessoa.class);
        Root<CadUnicoPessoa> root = criteria.from(CadUnicoPessoa.class);

        root.fetch("pesCidade", JoinType.LEFT);
        root.fetch("pesDistrito", JoinType.LEFT);

        Predicate[] predicates = criarRestricoes(cadUnicoPessoa, builder, root);
        criteria.where(predicates).distinct(true);

        TypedQuery<CadUnicoPessoa> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            CadUnicoPessoaFilter cadUnicoPessoa, CriteriaBuilder builder, Root<CadUnicoPessoa> root) {

        List<Predicate> predicates = new ArrayList<>();

        if (cadUnicoPessoa.getId() != null) {
            predicates.add(builder.equal(root.get(CadUnicoPessoa_.ID), cadUnicoPessoa.getId()));
        }

        if (cadUnicoPessoa.getCd_origem() != null) {
            predicates.add(builder.equal(root.get(CadUnicoPessoa_.CD_ORIGEM), cadUnicoPessoa.getCd_origem()));
        }

        if (StringUtils.hasText(cadUnicoPessoa.getNome())) {
            predicates.add(
                    builder.like(
                            builder.upper(root.get(CadUnicoPessoa_.NOME)),
                            "%" + cadUnicoPessoa.getNome().trim().toUpperCase() + "%"
                    )
            );
        }

        if (cadUnicoPessoa.getCpf() != null) {
            predicates.add(builder.equal(root.get(CadUnicoPessoa_.CPF_CNPJ), cadUnicoPessoa.getCpf()));
            predicates.add(builder.equal(root.get(CadUnicoPessoa_.FISICA_JURIDICA), "F"));
        }

        if (cadUnicoPessoa.getCnpj() != null) {
            predicates.add(builder.equal(root.get(CadUnicoPessoa_.CPF_CNPJ), cadUnicoPessoa.getCnpj()));
            predicates.add(builder.equal(root.get(CadUnicoPessoa_.FISICA_JURIDICA), "J"));
        }

        if (cadUnicoPessoa.getDataNascimento() != null) {
            LocalDateTime ini = cadUnicoPessoa.getDataNascimento().atStartOfDay();
            LocalDateTime fim = cadUnicoPessoa.getDataNascimento().plusDays(1).atStartOfDay();

            predicates.add(builder.greaterThanOrEqualTo(root.get(CadUnicoPessoa_.DATA_NASCIMENTO), ini));
            predicates.add(builder.lessThan(root.get(CadUnicoPessoa_.DATA_NASCIMENTO), fim));
        }

        return predicates.toArray(new Predicate[0]);
    }

    private void adicionarRestricoesDePaginacao(TypedQuery<?> query, Pageable pageable) {
        int paginaAtual = pageable.getPageNumber();
        int totalRegistrosPorPagina = pageable.getPageSize();
        int primeiroRegistroDaPagina = paginaAtual * totalRegistrosPorPagina;

        query.setFirstResult(primeiroRegistroDaPagina);
        query.setMaxResults(totalRegistrosPorPagina);
    }

    private Long total(CadUnicoPessoaFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<CadUnicoPessoa> root = criteria.from(CadUnicoPessoa.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}