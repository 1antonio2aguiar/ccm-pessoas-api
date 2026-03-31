package br.com.cc.pessoas.unificacao.pesRepository.pesTipoLogradouro;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoLogradouro;
import br.com.cc.pessoas.unificacao.pesEntity.PesTipoLogradouro_;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoLogradouroFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;

import java.util.ArrayList;
import java.util.List;

public class PesTipoLogradouroRepositoryImpl implements PesTipoLogradouroRepositoryQuery {
    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<PesTipoLogradouro> filtrar(PesTipoLogradouroFilter pesTipoLogradouro, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesTipoLogradouro> criteria = builder.createQuery(PesTipoLogradouro.class);
        Root<PesTipoLogradouro> root = criteria.from(PesTipoLogradouro.class);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(pesTipoLogradouro, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<PesTipoLogradouro> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(pesTipoLogradouro));

    }

    //Aqui da lista sem paginacao
    @Override
    public List<PesTipoLogradouro> filtrar(PesTipoLogradouroFilter pesTipoLogradouro) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesTipoLogradouro> criteria = builder.createQuery(PesTipoLogradouro.class);
        Root<PesTipoLogradouro> root = criteria.from(PesTipoLogradouro.class);

        Predicate[] predicates = criarRestricoes(pesTipoLogradouro, builder, root);
        criteria.where(predicates);

        TypedQuery<PesTipoLogradouro> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesTipoLogradouroFilter pesTipoLogradouro, CriteriaBuilder builder, Root<PesTipoLogradouro> root) {

        List<Predicate> predicates = new ArrayList<>();

        // POR TIPO LOGRADOURO
        if(pesTipoLogradouro.getTipoLogradouro() != null) {
            predicates.add(builder.equal(root.get(PesTipoLogradouro_.TIPO_LOGRADOURO), pesTipoLogradouro.getTipoLogradouro()));
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

    private Long total(PesTipoLogradouroFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesTipoLogradouro> root = criteria.from(PesTipoLogradouro.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}
