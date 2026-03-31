package br.com.cc.pessoas.unificacao.pesRepository.pesCidade;

import br.com.cc.pessoas.entity.Estado_;
import br.com.cc.pessoas.unificacao.pesEntity.PesCidade;
import br.com.cc.pessoas.unificacao.pesEntity.PesCidade_;
import br.com.cc.pessoas.unificacao.pesFilter.PesCidadeFilter;
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

public class PesCidadeRepositoryImpl implements PesCidadeRepositoryQuery {
    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<PesCidade> filtrar(PesCidadeFilter pesCidadeFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesCidade> criteria = builder.createQuery(PesCidade.class);
        Root<PesCidade> root = criteria.from(PesCidade.class);

        root.fetch("estado", JoinType.INNER);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(pesCidadeFilter, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<PesCidade> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(pesCidadeFilter));

    }

    //Aqui da lista sem paginacao
    @Override
    public List<PesCidade> filtrar(PesCidadeFilter pesCidadeFilter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesCidade> criteria = builder.createQuery(PesCidade.class);
        Root<PesCidade> root = criteria.from(PesCidade.class);

        root.fetch("estado", JoinType.INNER);

        Predicate[] predicates = criarRestricoes(pesCidadeFilter, builder, root);
        criteria.where(predicates);

        TypedQuery<PesCidade> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesCidadeFilter pesCidadeFilter, CriteriaBuilder builder, Root<PesCidade> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID DA CIDADE
        if(pesCidadeFilter.getCidade() != null) {
            predicates.add(builder.equal(root.get(PesCidade_.CIDADE), pesCidadeFilter.getCidade()));
        }

        // ID ESTADO
        if(pesCidadeFilter.getEstadoId() != null) {
            predicates.add(builder.equal(root.get(PesCidade_.ESTADO).get(Estado_.ID), pesCidadeFilter.getEstadoId()));
        }

        // NOME DA CDIADE
        if (StringUtils.hasLength(pesCidadeFilter.getNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get(PesCidade_.NOME)),
                            "%" + pesCidadeFilter.getNome().toLowerCase() + "%"));
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

    private Long total(PesCidadeFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesCidade> root = criteria.from(PesCidade.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}
