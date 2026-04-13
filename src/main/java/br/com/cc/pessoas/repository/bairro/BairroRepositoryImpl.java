package br.com.cc.pessoas.repository.bairro;

import br.com.cc.pessoas.entity.*;
import br.com.cc.pessoas.filter.BairroFilter;
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

public class BairroRepositoryImpl implements BairroRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public List<Bairro> filtrar(BairroFilter filter) {
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Bairro> cq = cb.createQuery(Bairro.class);
        Root<Bairro> root = cq.from(Bairro.class);

        root.fetch("distrito", JoinType.INNER)
            .fetch("cidade", JoinType.INNER);

        Predicate[] predicates = criarRestricoes(filter, cb, root);
        cq.where(predicates);

        return manager.createQuery(cq).getResultList();
    }

    @Override
    public Page<Bairro> filtrar(BairroFilter filter, Pageable pageable) {
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Bairro> cq = cb.createQuery(Bairro.class);
        Root<Bairro> root = cq.from(Bairro.class);

        root.fetch("distrito", JoinType.INNER)
                .fetch("cidade", JoinType.INNER);


        cq.where(criarRestricoes(filter, cb, root));
        cq.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));

        TypedQuery<Bairro> query = manager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(query.getResultList(), pageable, total(filter));
    }

    private Predicate[] criarRestricoes(
            BairroFilter filter, CriteriaBuilder cb, Root<Bairro> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID DO BAIRRO
        if (filter.getId() != null) {
            predicates.add(cb.equal(root.get("bairro"), filter.getId()));
        }

        // NOME DO BAIRRO
        if (StringUtils.hasText(filter.getNome())) {
            predicates.add(cb.like(
                    cb.lower(root.get("nome")),
                    "%" + filter.getNome().toLowerCase() + "%"
            ));
        }

        // ID DA CIDADE
        if (filter.getCidadeId() != null) {
            predicates.add(cb.equal(
                    root.get(Logradouro_.DISTRITO)
                            .get(Distrito_.CIDADE)
                            .get(Cidade_.ID),
                    filter.getCidadeId()));
        }

        // NOME DA CIDADE
        if (StringUtils.hasLength(filter.getCidadeNome())) {
            predicates.add(
                cb.like(
                    cb.lower(root.get(Bairro_.DISTRITO).get(Distrito_.CIDADE).get(Cidade_.NOME)),
                    "%" + filter.getCidadeNome().toLowerCase() + "%"));
        }

        return predicates.toArray(new Predicate[0]);
    }

    private Long total(BairroFilter filter) {
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Bairro> root = cq.from(Bairro.class);

        cq.select(cb.count(root));
        cq.where(criarRestricoes(filter, cb, root));

        return manager.createQuery(cq).getSingleResult();
    }
}