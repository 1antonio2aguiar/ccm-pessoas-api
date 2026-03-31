package br.com.cc.pessoas.repository.contato;

import br.com.cc.pessoas.entity.Contato;
import br.com.cc.pessoas.entity.Contato_;
import br.com.cc.pessoas.entity.Estado_;
import br.com.cc.pessoas.filter.ContatoFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

public class ContatoRepositoryImpl implements ContatoRepositoryQuery {
    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<Contato> filtrar(ContatoFilter contatoFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Contato> criteria = builder.createQuery(Contato.class);
        Root<Contato> root = criteria.from(Contato.class);

        root.fetch("pessoa", JoinType.INNER);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(contatoFilter, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<Contato> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(contatoFilter));

    }

    //Aqui da lista sem paginacao
    @Override
    public List<Contato> filtrar(ContatoFilter contatoFilter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Contato> criteria = builder.createQuery(Contato.class);
        Root<Contato> root = criteria.from(Contato.class);

        root.fetch("pessoa", JoinType.INNER);

        Predicate[] predicates = criarRestricoes(contatoFilter, builder, root);
        criteria.where(predicates);

        TypedQuery<Contato> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            ContatoFilter contatoFilter, CriteriaBuilder builder, Root<Contato> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID DO CONTATO
        if(contatoFilter.getId() != null) {
            predicates.add(builder.equal(root.get(Contato_.ID), contatoFilter.getId()));
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

    private Long total(ContatoFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Contato> root = criteria.from(Contato.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}
