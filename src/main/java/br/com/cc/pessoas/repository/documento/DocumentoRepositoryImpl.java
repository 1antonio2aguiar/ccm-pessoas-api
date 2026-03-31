package br.com.cc.pessoas.repository.documento;

import br.com.cc.pessoas.entity.Documento;
import br.com.cc.pessoas.entity.Documento_;
import br.com.cc.pessoas.filter.DocumentoFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;

import java.util.ArrayList;
import java.util.List;

public class DocumentoRepositoryImpl implements DocumentoRepositoryQuery {
    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<Documento> filtrar(DocumentoFilter documentoFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Documento> criteria = builder.createQuery(Documento.class);
        Root<Documento> root = criteria.from(Documento.class);

        root.fetch("pessoa", JoinType.INNER);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(documentoFilter, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<Documento> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(documentoFilter));

    }

    //Aqui da lista sem paginacao
    @Override
    public List<Documento> filtrar(DocumentoFilter documentoFilter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Documento> criteria = builder.createQuery(Documento.class);
        Root<Documento> root = criteria.from(Documento.class);

        root.fetch("pessoa", JoinType.INNER);

        Predicate[] predicates = criarRestricoes(documentoFilter, builder, root);
        criteria.where(predicates);

        TypedQuery<Documento> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            DocumentoFilter documentoFilter, CriteriaBuilder builder, Root<Documento> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID DO CONTATO
        if(documentoFilter.getId() != null) {
            predicates.add(builder.equal(root.get(Documento_.ID), documentoFilter.getId()));
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

    private Long total(DocumentoFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Documento> root = criteria.from(Documento.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}
