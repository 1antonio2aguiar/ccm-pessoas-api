package br.com.cc.pessoas.unificacao.pesRepository.pesCep;

import br.com.cc.pessoas.unificacao.pesEntity.PesCep;
import br.com.cc.pessoas.unificacao.pesEntity.PesCep_;
import br.com.cc.pessoas.unificacao.pesFilter.PesCepFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PesCepRepositoryImpl implements PesCepRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<PesCep> filtrar(PesCepFilter pesCepFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesCep> criteria = builder.createQuery(PesCep.class);
        Root<PesCep> root = criteria.from(PesCep.class);

        root.fetch("pesLogradouro", JoinType.LEFT)
                .fetch("pesDistrito", JoinType.LEFT)
                .fetch("pesCidade", JoinType.LEFT)
                .fetch("estado", JoinType.LEFT);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(pesCepFilter, builder, root);
        criteria.where(predicates).orderBy(orders).distinct(true);

        TypedQuery<PesCep> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(pesCepFilter));
    }

    @Override
    public List<PesCep> filtrar(PesCepFilter pesCepFilter) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesCep> criteria = builder.createQuery(PesCep.class);
        Root<PesCep> root = criteria.from(PesCep.class);

        root.fetch("pesLogradouro", JoinType.LEFT)
                .fetch("pesDistrito", JoinType.LEFT)
                .fetch("pesCidade", JoinType.LEFT)
                .fetch("estado", JoinType.LEFT);

        Predicate[] predicates = criarRestricoes(pesCepFilter, builder, root);
        criteria.where(predicates).distinct(true);

        TypedQuery<PesCep> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesCepFilter filter, CriteriaBuilder builder, Root<PesCep> root) {

        List<Predicate> predicates = new ArrayList<>();

        // CEP
        if (filter.getCep() != null) {
            predicates.add(builder.equal(root.get(PesCep_.CEP), filter.getCep()));
        }

        // ID CIDADE
        if (filter.getCidade() != null) {
            predicates.add(builder.equal(root.get(PesCep_.CIDADE), filter.getCidade()));
        }

        // NOME DA CIDADE
        if (StringUtils.hasText(filter.getCidadeNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get("pesLogradouro")
                                    .get("pesDistrito")
                                    .get("pesCidade")
                                    .get("nome")),
                            "%" + filter.getCidadeNome().toLowerCase() + "%"
                    )
            );
        }

        // ID LOGRADOURO
        if (filter.getLogradouro() != null) {
            predicates.add(builder.equal(root.get(PesCep_.LOGRADOURO), filter.getLogradouro()));
        }

        // NOME DO LOGRADOURO
        if (StringUtils.hasText(filter.getLogradouroNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get("pesLogradouro").get("nome")),
                            "%" + filter.getLogradouroNome().toLowerCase() + "%"
                    )
            );
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

    private Long total(PesCepFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesCep> root = criteria.from(PesCep.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}