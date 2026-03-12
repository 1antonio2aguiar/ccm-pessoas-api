package br.com.cc.pessoas.repository.cep;

import br.com.cc.pessoas.entity.*;
import br.com.cc.pessoas.filter.CepFilter;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CepRepositoryImpl implements CepRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public List<Cep> filtrar(CepFilter filter) {
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Cep> cq = cb.createQuery(Cep.class);
        Root<Cep> root = cq.from(Cep.class);

        root.fetch(Cep_.LOGRADOURO, JoinType.INNER)
                .fetch(Logradouro_.TIPO_LOGRADOURO, JoinType.INNER);

        root.fetch(Cep_.LOGRADOURO, JoinType.INNER)
                .fetch(Logradouro_.DISTRITO, JoinType.INNER)
                .fetch(Distrito_.CIDADE, JoinType.INNER)
                .fetch(Cidade_.ESTADO, JoinType.INNER);

        root.fetch(Cep_.BAIRRO, JoinType.INNER)
                .fetch(Bairro_.DISTRITO, JoinType.INNER)
                .fetch(Distrito_.CIDADE, JoinType.INNER)
                .fetch(Cidade_.ESTADO, JoinType.INNER);

        cq.where(criarRestricoes(filter, cb, root));
        return manager.createQuery(cq).getResultList();
    }

    @Override
    public Page<Cep> filtrar(CepFilter filter, Pageable pageable) {
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Cep> cq = cb.createQuery(Cep.class);
        Root<Cep> root = cq.from(Cep.class);

        root.fetch(Cep_.LOGRADOURO, JoinType.INNER)
                .fetch(Logradouro_.TIPO_LOGRADOURO, JoinType.INNER);

        root.fetch(Cep_.LOGRADOURO, JoinType.INNER)
                .fetch(Logradouro_.DISTRITO, JoinType.INNER)
                .fetch(Distrito_.CIDADE, JoinType.INNER)
                .fetch(Cidade_.ESTADO, JoinType.INNER);

        root.fetch(Cep_.BAIRRO, JoinType.INNER)
                .fetch(Bairro_.DISTRITO, JoinType.INNER)
                .fetch(Distrito_.CIDADE, JoinType.INNER)
                .fetch(Cidade_.ESTADO, JoinType.INNER);

        cq.where(criarRestricoes(filter, cb, root));
        cq.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));

        TypedQuery<Cep> query = manager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(query.getResultList(), pageable, total(filter));
    }

    @Override
    public List<String> buscarCepsPorLogradouroBairroNumero(Long logradouroId, Long bairroId, Integer numero) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<String> criteria = builder.createQuery(String.class);
        Root<Cep> root = criteria.from(Cep.class);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("logradouro").get("id"), logradouroId));
        predicates.add(builder.equal(root.get("bairro").get("id"), bairroId));
        predicates.add(builder.lessThanOrEqualTo(root.get("numeroIni"), numero));
        predicates.add(builder.greaterThanOrEqualTo(root.get("numeroFim"), numero));

        criteria.select(root.get("cep"));
        criteria.where(predicates.toArray(new Predicate[0]));
        criteria.distinct(true);
        criteria.orderBy(builder.asc(root.get("cep")));

        TypedQuery<String> query = manager.createQuery(criteria);
        return query.getResultList();
    }


    private Predicate[] criarRestricoes(
            CepFilter filter, CriteriaBuilder cb, Root<Cep> root) {

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getId() != null) {
            predicates.add(cb.equal(root.get(Cep_.ID), filter.getId()));
        }

        if (StringUtils.hasText(filter.getCep())) {
            predicates.add(
                    cb.equal(
                            cb.upper(cb.trim(root.get(Cep_.CEP))),
                            filter.getCep().trim().toUpperCase()
                    )
            );
        }

        if (filter.getCidadeId() != null) {
            predicates.add(cb.equal(
                    root.get(Cep_.BAIRRO)
                            .get(Bairro_.DISTRITO)
                            .get(Distrito_.CIDADE)
                            .get(Cidade_.ID),
                    filter.getCidadeId()
            ));
        }

        if (StringUtils.hasText(filter.getCidadeNome())) {
            predicates.add(cb.like(
                    cb.lower(
                            root.get(Cep_.BAIRRO)
                                    .get(Bairro_.DISTRITO)
                                    .get(Distrito_.CIDADE)
                                    .get(Cidade_.NOME)
                    ),
                    "%" + filter.getCidadeNome().toLowerCase() + "%"
            ));
        }

        if (StringUtils.hasText(filter.getBairroNome())) {
            predicates.add(cb.like(
                    cb.lower(root.get(Cep_.BAIRRO).get(Bairro_.NOME)),
                    "%" + filter.getBairroNome().toLowerCase() + "%"
            ));
        }

        if (StringUtils.hasText(filter.getLogradouroNome())) {
            predicates.add(cb.like(
                    cb.lower(root.get(Cep_.LOGRADOURO).get(Logradouro_.NOME)),
                    "%" + filter.getLogradouroNome().toLowerCase() + "%"
            ));
        }

        return predicates.toArray(new Predicate[0]);
    }

    private Long total(CepFilter filter) {
        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Cep> root = cq.from(Cep.class);

        cq.select(cb.count(root));
        cq.where(criarRestricoes(filter, cb, root));

        return manager.createQuery(cq).getSingleResult();
    }
}
