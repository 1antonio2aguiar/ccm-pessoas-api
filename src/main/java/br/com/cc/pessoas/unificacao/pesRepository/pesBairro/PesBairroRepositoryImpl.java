package br.com.cc.pessoas.unificacao.pesRepository.pesBairro;

import br.com.cc.pessoas.unificacao.pesEntity.PesBairro;
import br.com.cc.pessoas.unificacao.pesEntity.PesBairro_;
import br.com.cc.pessoas.unificacao.pesFilter.PesBairroFilter;
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

public class PesBairroRepositoryImpl implements PesBairroRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<PesBairro> filtrar(PesBairroFilter pesBairroFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesBairro> criteria = builder.createQuery(PesBairro.class);
        Root<PesBairro> root = criteria.from(PesBairro.class);

        root.fetch("pesDistrito", JoinType.LEFT)
                .fetch("pesCidade", JoinType.LEFT)
                .fetch("estado", JoinType.LEFT);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(pesBairroFilter, builder, root);
        criteria.where(predicates).orderBy(orders).distinct(true);

        TypedQuery<PesBairro> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(pesBairroFilter));
    }

    @Override
    public List<PesBairro> filtrar(PesBairroFilter pesBairroFilter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesBairro> criteria = builder.createQuery(PesBairro.class);
        Root<PesBairro> root = criteria.from(PesBairro.class);

        root.fetch("pesDistrito", JoinType.LEFT)
                .fetch("pesCidade", JoinType.LEFT)
                .fetch("estado", JoinType.LEFT);

        Predicate[] predicates = criarRestricoes(pesBairroFilter, builder, root);
        criteria.where(predicates).distinct(true);

        TypedQuery<PesBairro> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesBairroFilter pesBairroFilter, CriteriaBuilder builder, Root<PesBairro> root) {

        List<Predicate> predicates = new ArrayList<>();

        // BAIRRO
        if (pesBairroFilter.getBairro() != null) {
            predicates.add(builder.equal(root.get(PesBairro_.BAIRRO), pesBairroFilter.getBairro()));
        }

        // NOME DO BAIRRO
        if (StringUtils.hasLength(pesBairroFilter.getNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get(PesBairro_.NOME)),
                            "%" + pesBairroFilter.getNome().toLowerCase() + "%"
                    )
            );
        }

        // CIDADE
        if (pesBairroFilter.getCidade() != null) {
            predicates.add(builder.equal(root.get(PesBairro_.CIDADE), pesBairroFilter.getCidade()));
        }

        // NOME DA CIDADE
        if (StringUtils.hasLength(pesBairroFilter.getCidadeNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get("pesDistrito").get("pesCidade").get("nome")),
                            "%" + pesBairroFilter.getCidadeNome().toLowerCase() + "%"
                    )
            );
        }

        // DISTRITO
        if (pesBairroFilter.getDistrito() != null) {
            predicates.add(builder.equal(root.get(PesBairro_.DISTRITO), pesBairroFilter.getDistrito()));
        }

        // NOME DO DISTRITO
        if (StringUtils.hasLength(pesBairroFilter.getDistritoNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get("pesDistrito").get("nome")),
                            "%" + pesBairroFilter.getDistritoNome().toLowerCase() + "%"
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

    private Long total(PesBairroFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesBairro> root = criteria.from(PesBairro.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}