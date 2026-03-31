package br.com.cc.pessoas.unificacao.pesRepository.pesLogradouro;

import br.com.cc.pessoas.unificacao.pesEntity.PesLogradouro;
import br.com.cc.pessoas.unificacao.pesEntity.PesLogradouro_;
import br.com.cc.pessoas.unificacao.pesFilter.PesLogradouroFilter;
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

public class PesLogradouroRepositoryImpl implements PesLogradouroRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<PesLogradouro> filtrar(PesLogradouroFilter pesLogradouroFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesLogradouro> criteria = builder.createQuery(PesLogradouro.class);
        Root<PesLogradouro> root = criteria.from(PesLogradouro.class);

        root.fetch("pesDistrito", JoinType.LEFT)
                .fetch("pesCidade", JoinType.LEFT)
                .fetch("estado", JoinType.LEFT);

        root.fetch("tipoLogradouro", JoinType.INNER);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(pesLogradouroFilter, builder, root);
        criteria.where(predicates).orderBy(orders).distinct(true);

        TypedQuery<PesLogradouro> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(pesLogradouroFilter));
    }

    @Override
    public List<PesLogradouro> filtrar(PesLogradouroFilter pesLogradouroFilter) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesLogradouro> criteria = builder.createQuery(PesLogradouro.class);
        Root<PesLogradouro> root = criteria.from(PesLogradouro.class);

        root.fetch("pesDistrito", JoinType.LEFT)
                .fetch("pesCidade", JoinType.LEFT)
                .fetch("estado", JoinType.LEFT);

        root.fetch("tipoLogradouro", JoinType.INNER);

        Predicate[] predicates = criarRestricoes(pesLogradouroFilter, builder, root);
        criteria.where(predicates).distinct(true);

        TypedQuery<PesLogradouro> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesLogradouroFilter pesLogradouroFilter, CriteriaBuilder builder, Root<PesLogradouro> root) {

        List<Predicate> predicates = new ArrayList<>();

        if (pesLogradouroFilter.getLogradouro() != null) {
            predicates.add(builder.equal(root.get(PesLogradouro_.LOGRADOURO), pesLogradouroFilter.getLogradouro()));
        }

        if (StringUtils.hasLength(pesLogradouroFilter.getNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get(PesLogradouro_.NOME)),
                            "%" + pesLogradouroFilter.getNome().toLowerCase() + "%"
                    )
            );
        }

        if (pesLogradouroFilter.getCidade() != null) {
            predicates.add(builder.equal(root.get(PesLogradouro_.CIDADE), pesLogradouroFilter.getCidade()));
        }

        if (StringUtils.hasLength(pesLogradouroFilter.getCidadeNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get("pesDistrito").get("pesCidade").get("nome")),
                            "%" + pesLogradouroFilter.getCidadeNome().toLowerCase() + "%"
                    )
            );
        }

        if (pesLogradouroFilter.getDistrito() != null) {
            predicates.add(builder.equal(root.get(PesLogradouro_.DISTRITO), pesLogradouroFilter.getDistrito()));
        }

        if (StringUtils.hasLength(pesLogradouroFilter.getDistritoNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get("pesDistrito").get("nome")),
                            "%" + pesLogradouroFilter.getDistritoNome().toLowerCase() + "%"
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

    private Long total(PesLogradouroFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesLogradouro> root = criteria.from(PesLogradouro.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}