package br.com.cc.pessoas.repository.distrito;

import br.com.cc.pessoas.entity.Cidade_;
import br.com.cc.pessoas.entity.Distrito_;
import br.com.cc.pessoas.filter.DistritoFilter;
import br.com.cc.pessoas.entity.Distrito;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.util.StringUtils;

import java.util.*;

public class DistritoRepositoryImpl implements DistritoRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<Distrito> filtrar(DistritoFilter filter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Distrito> criteria = builder.createQuery(Distrito.class);
        Root<Distrito> root = criteria.from(Distrito.class);

        root.fetch("cidade", JoinType.INNER).fetch("estado", JoinType.INNER);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates)
                .orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));

        TypedQuery<Distrito> query = manager.createQuery(criteria);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(query.getResultList(), pageable, total(filter));
    }

    @Override
    public List<Distrito> filtrar(DistritoFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Distrito> criteria = builder.createQuery(Distrito.class);
        Root<Distrito> root = criteria.from(Distrito.class);

        root.fetch("cidade", JoinType.INNER).fetch("estado", JoinType.INNER);

        criteria.where(criarRestricoes(filter, builder, root));
        return manager.createQuery(criteria).getResultList();
    }

    private Predicate[] criarRestricoes(DistritoFilter filter, CriteriaBuilder builder, Root<Distrito> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID DO DISTRITO
        if(filter.getId() != null) {
            predicates.add(builder.equal(root.get(Distrito_.ID), filter.getId()));
        }

        // NOME DO DISTRITO
        if (StringUtils.hasLength(filter.getNome())) {
            predicates.add(
                builder.like(
                    builder.lower(root.get(Distrito_.NOME)),
                    "%" + filter.getNome().toLowerCase() + "%"));
        }

        // ID DA CIDADE
        if (filter.getCidadeId() != null) {
            predicates.add(builder.equal(root.get(Distrito_.CIDADE).get(Cidade_.ID), filter.getCidadeId()));
        }

        // NOME DA CIDADE
        if (StringUtils.hasLength(filter.getCidadeNome())) {
            predicates.add(builder.like(
                    builder.lower(root.get(Distrito_.CIDADE).get(Cidade_.NOME)),
                    "%" + filter.getCidadeNome().toLowerCase() + "%"));
        }

        return predicates.toArray(new Predicate[0]);
    }

    private Long total(DistritoFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Distrito> root = criteria.from(Distrito.class);

        criteria.select(builder.count(root));
        criteria.where(criarRestricoes(filter, builder, root));

        return manager.createQuery(criteria).getSingleResult();
    }
}
