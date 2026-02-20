package br.com.cc.pessoas.repository.tipoLogradouro;

import br.com.cc.pessoas.entity.TipoLogradouro;
import br.com.cc.pessoas.entity.TipoLogradouro_;
import br.com.cc.pessoas.filter.TipoLogradouroFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TipoLogradouroRepositoryImpl implements TipoLogradouroRepositoryQuery {
    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<TipoLogradouro> filtrar(TipoLogradouroFilter tipoLogradouroFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<TipoLogradouro> criteria = builder.createQuery(TipoLogradouro.class);
        Root<TipoLogradouro> root = criteria.from(TipoLogradouro.class);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(tipoLogradouroFilter, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<TipoLogradouro> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(tipoLogradouroFilter));

    }

    //Aqui da lista sem paginacao
    @Override
    public List<TipoLogradouro> filtrar(TipoLogradouroFilter tipoLogradouroFilter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<TipoLogradouro> criteria = builder.createQuery(TipoLogradouro.class);
        Root<TipoLogradouro> root = criteria.from(TipoLogradouro.class);

        Predicate[] predicates = criarRestricoes(tipoLogradouroFilter, builder, root);
        criteria.where(predicates);

        TypedQuery<TipoLogradouro> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            TipoLogradouroFilter tipoLogradouroFilter, CriteriaBuilder builder, Root<TipoLogradouro> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID
        if(tipoLogradouroFilter.getId() != null) {
            predicates.add(builder.equal(root.get(TipoLogradouro_.ID), tipoLogradouroFilter.getId()));
        }

        // NOME
        if (StringUtils.hasLength(tipoLogradouroFilter.getDescricao())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get(TipoLogradouro_.DESCRICAO)),
                            "%" + tipoLogradouroFilter.getDescricao().toLowerCase() + "%"));
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

    private Long total(TipoLogradouroFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<TipoLogradouro> root = criteria.from(TipoLogradouro.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}