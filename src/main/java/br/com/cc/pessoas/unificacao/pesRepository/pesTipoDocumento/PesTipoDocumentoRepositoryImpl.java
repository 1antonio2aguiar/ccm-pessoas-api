package br.com.cc.pessoas.unificacao.pesRepository.pesTipoDocumento;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoDocumento;
import br.com.cc.pessoas.unificacao.pesEntity.PesTipoDocumento_;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoDocumentoFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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

public class PesTipoDocumentoRepositoryImpl implements PesTipoDocumentoRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<PesTipoDocumento> filtrar(PesTipoDocumentoFilter filter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesTipoDocumento> criteria = builder.createQuery(PesTipoDocumento.class);
        Root<PesTipoDocumento> root = criteria.from(PesTipoDocumento.class);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<PesTipoDocumento> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(filter));
    }

    @Override
    public List<PesTipoDocumento> filtrar(PesTipoDocumentoFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesTipoDocumento> criteria = builder.createQuery(PesTipoDocumento.class);
        Root<PesTipoDocumento> root = criteria.from(PesTipoDocumento.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        TypedQuery<PesTipoDocumento> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesTipoDocumentoFilter filter, CriteriaBuilder builder, Root<PesTipoDocumento> root) {

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getTipoDocumento() != null) {
            predicates.add(builder.equal(root.get(PesTipoDocumento_.TIPO_DOCUMENTO), filter.getTipoDocumento()));
        }

        if (StringUtils.hasText(filter.getDescricao())) {
            predicates.add(
                    builder.like(
                            builder.upper(root.get(PesTipoDocumento_.DESCRICAO)),
                            "%" + filter.getDescricao().trim().toUpperCase() + "%"
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

    private Long total(PesTipoDocumentoFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesTipoDocumento> root = criteria.from(PesTipoDocumento.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}