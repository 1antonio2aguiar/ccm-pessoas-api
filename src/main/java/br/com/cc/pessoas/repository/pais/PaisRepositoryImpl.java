package br.com.cc.pessoas.repository.pais;

import java.util.ArrayList;
import java.util.List;

import br.com.cc.pessoas.entity.Pais_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.util.StringUtils;

import br.com.cc.pessoas.entity.Pais;
import br.com.cc.pessoas.filter.PaisFilter;

public class PaisRepositoryImpl implements PaisRepositoryQuery {
    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<Pais> filtrar(PaisFilter paisFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Pais> criteria = builder.createQuery(Pais.class);
        Root<Pais> root = criteria.from(Pais.class);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(paisFilter, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<Pais> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(paisFilter));

    }

    //Aqui da lista sem paginacao
    @Override
    public List<Pais> filtrar(PaisFilter paisFilter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Pais> criteria = builder.createQuery(Pais.class);
        Root<Pais> root = criteria.from(Pais.class);

        Predicate[] predicates = criarRestricoes(paisFilter, builder, root);
        criteria.where(predicates);

        TypedQuery<Pais> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PaisFilter paisFilter, CriteriaBuilder builder, Root<Pais> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID
        if(paisFilter.getId() != null) {
            predicates.add(builder.equal(root.get(Pais_.ID), paisFilter.getId()));
        }

        // NOME
        if (StringUtils.hasLength(paisFilter.getNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get(Pais_.NOME)),
                            "%" + paisFilter.getNome().toLowerCase() + "%"));
        }

        return predicates.toArray(new Predicate[predicates.size()]);
    }
    private void adicionarRestricoesDePaginacao(TypedQuery<?> query, Pageable pageable) {
        int paginaAtual = pageable.getPageNumber();
        int totalRegistrosPorPagina = pageable.getPageSize();
        int primeiroRegistroDaPagina = paginaAtual * totalRegistrosPorPagina;

        query.setFirstResult(primeiroRegistroDaPagina);
        query.setMaxResults(totalRegistrosPorPagina);
    }

    private Long total(PaisFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Pais> root = criteria.from(Pais.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}