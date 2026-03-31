package br.com.cc.pessoas.unificacao.pesRepository.pesDistrito;

import br.com.cc.pessoas.unificacao.pesEntity.PesDistrito;
import br.com.cc.pessoas.unificacao.pesEntity.PesDistrito_;
import br.com.cc.pessoas.unificacao.pesFilter.PesDistritoFilter;
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

public class PesDistritoRepositoryImpl implements PesDistritoRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<PesDistrito> filtrar(PesDistritoFilter pesDistritoFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesDistrito> criteria = builder.createQuery(PesDistrito.class);
        Root<PesDistrito> root = criteria.from(PesDistrito.class);

        root.fetch("pesCidade", JoinType.LEFT).fetch("estado", JoinType.LEFT);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(pesDistritoFilter, builder, root);
        criteria.where(predicates).orderBy(orders).distinct(true);

        TypedQuery<PesDistrito> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(pesDistritoFilter));
    }

    @Override
    public List<PesDistrito> filtrar(PesDistritoFilter pesDistritoFilter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesDistrito> criteria = builder.createQuery(PesDistrito.class);
        Root<PesDistrito> root = criteria.from(PesDistrito.class);

        root.fetch("pesCidade", JoinType.LEFT).fetch("estado", JoinType.LEFT);

        Predicate[] predicates = criarRestricoes(pesDistritoFilter, builder, root);
        criteria.where(predicates).distinct(true);

        TypedQuery<PesDistrito> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesDistritoFilter pesDistritoFilter, CriteriaBuilder builder, Root<PesDistrito> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID DO DISTRITO
        if (pesDistritoFilter.getDistrito() != null) {
            predicates.add(builder.equal(root.get(PesDistrito_.DISTRITO), pesDistritoFilter.getDistrito()));
        }

        // NOME DO DISTRITO
        if (StringUtils.hasLength(pesDistritoFilter.getNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get(PesDistrito_.NOME)),
                            "%" + pesDistritoFilter.getNome().toLowerCase() + "%"
                    )
            );
        }

        // ID DA CIDADE
        if (pesDistritoFilter.getCidade() != null) {
            predicates.add(builder.equal(root.get(PesDistrito_.CIDADE), pesDistritoFilter.getCidade()));
        }

        // NOME DA CIDADE
        if (StringUtils.hasLength(pesDistritoFilter.getCidadeNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get("pesCidade").get("nome")),
                            "%" + pesDistritoFilter.getCidadeNome().toLowerCase() + "%"
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

    private Long total(PesDistritoFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesDistrito> root = criteria.from(PesDistrito.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}