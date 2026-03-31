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
                .fetch("pesEstado", JoinType.LEFT);

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
                .fetch("pesEstado", JoinType.LEFT);

        Predicate[] predicates = criarRestricoes(pesCepFilter, builder, root);
        criteria.where(predicates).distinct(true);

        TypedQuery<PesCep> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesCepFilter pesCepFilter, CriteriaBuilder builder, Root<PesCep> root) {

        List<Predicate> predicates = new ArrayList<>();

        if (pesCepFilter.getCep() != null) {
            predicates.add(builder.equal(root.get(PesCep_.CEP), pesCepFilter.getCep()));
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