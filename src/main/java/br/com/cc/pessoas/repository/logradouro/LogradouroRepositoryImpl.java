package br.com.cc.pessoas.repository.logradouro;

import br.com.cc.pessoas.entity.Cidade_;
import br.com.cc.pessoas.entity.Distrito_;
import br.com.cc.pessoas.entity.Logradouro_;
import br.com.cc.pessoas.filter.LogradouroFilter;
import br.com.cc.pessoas.entity.Logradouro;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class LogradouroRepositoryImpl implements LogradouroRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public List<Logradouro> filtrar(LogradouroFilter filter) {
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Logradouro> cq = cb.createQuery(Logradouro.class);
        Root<Logradouro> root = cq.from(Logradouro.class);

        root.fetch(Logradouro_.DISTRITO, JoinType.INNER)
            .fetch(Distrito_.CIDADE, JoinType.INNER);

        root.fetch(Logradouro_.TIPO_LOGRADOURO, JoinType.INNER);

        cq.where(criarRestricoes(filter, cb, root));
        return manager.createQuery(cq).getResultList();
    }

    @Override
    public Page<Logradouro> filtrar(LogradouroFilter filter, Pageable pageable) {
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Logradouro> cq = cb.createQuery(Logradouro.class);
        Root<Logradouro> root = cq.from(Logradouro.class);

        root.fetch(Logradouro_.DISTRITO, JoinType.INNER)
            .fetch(Distrito_.CIDADE, JoinType.INNER);

        root.fetch(Logradouro_.TIPO_LOGRADOURO, JoinType.INNER);

        cq.where(criarRestricoes(filter, cb, root));
        cq.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));

        TypedQuery<Logradouro> query = manager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(query.getResultList(), pageable, total(filter));
    }

    private Predicate[] criarRestricoes(
            LogradouroFilter filter, CriteriaBuilder cb, Root<Logradouro> root) {

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getId() != null) {
            predicates.add(cb.equal(root.get(Logradouro_.ID), filter.getId()));
        }

        if (StringUtils.hasText(filter.getNome())) {
            predicates.add(cb.like(
                    cb.lower(root.get(Logradouro_.NOME)),
                    "%" + filter.getNome().toLowerCase() + "%"
            ));
        }

        if (filter.getCidadeId() != null) {
            predicates.add(cb.equal(
                    root.get(Logradouro_.DISTRITO)
                            .get(Distrito_.CIDADE)
                            .get(Cidade_.ID),
                    filter.getCidadeId()));
        }

        if (StringUtils.hasText(filter.getCidadeNome())) {
            predicates.add(cb.like(
                    cb.lower(
                            root.get(Logradouro_.DISTRITO)
                                    .get(Distrito_.CIDADE)
                                    .get(Cidade_.NOME)
                    ),
                    "%" + filter.getCidadeNome().toLowerCase() + "%"
            ));
        }

        return predicates.toArray(new Predicate[0]);
    }

    private Long total(LogradouroFilter filter) {
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Logradouro> root = cq.from(Logradouro.class);

        cq.select(cb.count(root));
        cq.where(criarRestricoes(filter, cb, root));

        return manager.createQuery(cq).getSingleResult();
    }
}
