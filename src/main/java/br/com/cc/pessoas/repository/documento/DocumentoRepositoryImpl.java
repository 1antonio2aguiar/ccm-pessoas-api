package br.com.cc.pessoas.repository.documento;

import br.com.cc.pessoas.entity.Documento;
import br.com.cc.pessoas.entity.Documento_;
import br.com.cc.pessoas.filter.DocumentoFilter;
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

import java.util.ArrayList;
import java.util.List;

public class DocumentoRepositoryImpl
        implements DocumentoRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<Documento> filtrar(
            DocumentoFilter documentoFilter,
            Pageable pageable) {

        CriteriaBuilder builder =
                manager.getCriteriaBuilder();

        CriteriaQuery<Documento> criteria =
                builder.createQuery(Documento.class);

        Root<Documento> root =
                criteria.from(Documento.class);

        root.fetch("pessoa", JoinType.INNER);
        root.fetch("dadosPessoaJuridica", JoinType.LEFT);

        Predicate[] predicates =
                criarRestricoes(
                        documentoFilter,
                        builder,
                        root
                );

        List<Order> orders =
                QueryUtils.toOrders(
                        pageable.getSort(),
                        root,
                        builder
                );

        criteria
                .select(root)
                .where(predicates)
                .orderBy(orders);

        TypedQuery<Documento> query =
                manager.createQuery(criteria);

        adicionarRestricoesDePaginacao(
                query,
                pageable
        );

        return new PageImpl<>(
                query.getResultList(),
                pageable,
                total(documentoFilter)
        );
    }

    @Override
    public List<Documento> filtrar(
            DocumentoFilter documentoFilter) {

        CriteriaBuilder builder =
                manager.getCriteriaBuilder();

        CriteriaQuery<Documento> criteria =
                builder.createQuery(Documento.class);

        Root<Documento> root =
                criteria.from(Documento.class);

        root.fetch("pessoa", JoinType.INNER);
        root.fetch("dadosPessoaJuridica", JoinType.LEFT);

        Predicate[] predicates =
                criarRestricoes(
                        documentoFilter,
                        builder,
                        root
                );

        criteria
                .select(root)
                .where(predicates);

        return manager
                .createQuery(criteria)
                .getResultList();
    }

    private Predicate[] criarRestricoes(
            DocumentoFilter documentoFilter,
            CriteriaBuilder builder,
            Root<Documento> root) {

        List<Predicate> predicates =
                new ArrayList<>();

        if (documentoFilter.getId() != null) {
            predicates.add(
                    builder.equal(
                            root.get(Documento_.ID),
                            documentoFilter.getId()
                    )
            );
        }

        return predicates.toArray(
                new Predicate[0]
        );
    }

    private void adicionarRestricoesDePaginacao(
            TypedQuery<?> query,
            Pageable pageable) {

        query.setFirstResult(
                (int) pageable.getOffset()
        );

        query.setMaxResults(
                pageable.getPageSize()
        );
    }

    private Long total(
            DocumentoFilter filter) {

        CriteriaBuilder builder =
                manager.getCriteriaBuilder();

        CriteriaQuery<Long> criteria =
                builder.createQuery(Long.class);

        Root<Documento> root =
                criteria.from(Documento.class);

        Predicate[] predicates =
                criarRestricoes(
                        filter,
                        builder,
                        root
                );

        criteria
                .select(builder.count(root))
                .where(predicates);

        return manager
                .createQuery(criteria)
                .getSingleResult();
    }
}