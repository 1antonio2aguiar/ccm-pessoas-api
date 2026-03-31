package br.com.cc.pessoas.unificacao.pesRepository.pesPais;

import br.com.cc.pessoas.unificacao.pesEntity.PesPais;
import br.com.cc.pessoas.unificacao.pesEntity.PesPais_;
import br.com.cc.pessoas.unificacao.pesFilter.PesPaisFilter;
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

public class PesPaisRepositoryImpl implements PesPaisRepositoryQuery {
    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<PesPais> filtrar(PesPaisFilter pesPaisFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesPais> criteria = builder.createQuery(PesPais.class);
        Root<PesPais> root = criteria.from(PesPais.class);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(pesPaisFilter, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<PesPais> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(pesPaisFilter));

    }

    //Aqui da lista sem paginacao
    @Override
    public List<PesPais> filtrar(PesPaisFilter pesPaisFilter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesPais> criteria = builder.createQuery(PesPais.class);
        Root<PesPais> root = criteria.from(PesPais.class);

        Predicate[] predicates = criarRestricoes(pesPaisFilter, builder, root);
        criteria.where(predicates);

        TypedQuery<PesPais> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesPaisFilter pesPaisFilter, CriteriaBuilder builder, Root<PesPais> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID DO PAIS
        if(pesPaisFilter.getPais() != null) {
            predicates.add(builder.equal(root.get(PesPais_.PAIS), pesPaisFilter.getPais()));
        }

        // NOME DO PAIS
        if (StringUtils.hasLength(pesPaisFilter.getNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get(PesPais_.NOME)),
                            "%" + pesPaisFilter.getNome().toLowerCase() + "%"));
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

    private Long total(PesPaisFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesPais> root = criteria.from(PesPais.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}
