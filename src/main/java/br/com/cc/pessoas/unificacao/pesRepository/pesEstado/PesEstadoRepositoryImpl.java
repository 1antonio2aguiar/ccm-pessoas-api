package br.com.cc.pessoas.unificacao.pesRepository.pesEstado;

import br.com.cc.pessoas.unificacao.pesEntity.PesEstado;
import br.com.cc.pessoas.unificacao.pesEntity.PesEstado_;
import br.com.cc.pessoas.unificacao.pesFilter.PesEstadoFilter;
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

public class PesEstadoRepositoryImpl implements PesEstadoRepositoryQuery {
    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<PesEstado> filtrar(PesEstadoFilter pesEstadoFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesEstado> criteria = builder.createQuery(PesEstado.class);
        Root<PesEstado> root = criteria.from(PesEstado.class);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(pesEstadoFilter, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<PesEstado> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(pesEstadoFilter));

    }

    //Aqui da lista sem paginacao
    @Override
    public List<PesEstado> filtrar(PesEstadoFilter pesEstadoFilter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesEstado> criteria = builder.createQuery(PesEstado.class);
        Root<PesEstado> root = criteria.from(PesEstado.class);

        Predicate[] predicates = criarRestricoes(pesEstadoFilter, builder, root);
        criteria.where(predicates);

        TypedQuery<PesEstado> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesEstadoFilter pesEstadoFilter, CriteriaBuilder builder, Root<PesEstado> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID DO ESTADO
        if(pesEstadoFilter.getEstado() != null) {
            predicates.add(builder.equal(root.get(PesEstado_.ESTADO), pesEstadoFilter.getEstado().toUpperCase()));
        }

        // NOME DO ESTADO
        if (StringUtils.hasLength(pesEstadoFilter.getDescricao())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get(PesEstado_.DESCRICAO)),
                            "%" + pesEstadoFilter.getDescricao().toLowerCase() + "%"));
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

    private Long total(PesEstadoFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesEstado> root = criteria.from(PesEstado.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}
