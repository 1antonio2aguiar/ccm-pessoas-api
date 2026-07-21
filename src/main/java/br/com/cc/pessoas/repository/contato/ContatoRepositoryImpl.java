package br.com.cc.pessoas.repository.contato;

import br.com.cc.pessoas.entity.Contato;
import br.com.cc.pessoas.entity.Contato_;
import br.com.cc.pessoas.filter.ContatoFilter;
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

public class ContatoRepositoryImpl
        implements ContatoRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<Contato> filtrar(
            ContatoFilter contatoFilter,
            Pageable pageable) {

        CriteriaBuilder builder =
                manager.getCriteriaBuilder();

        CriteriaQuery<Contato> criteria =
                builder.createQuery(Contato.class);

        Root<Contato> root =
                criteria.from(Contato.class);

        root.fetch("pessoa", JoinType.INNER);
        root.fetch("dadosPessoaJuridica", JoinType.LEFT);

        Predicate[] predicates =
                criarRestricoes(contatoFilter, builder, root);

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

        TypedQuery<Contato> query =
                manager.createQuery(criteria);

        adicionarRestricoesDePaginacao(
                query,
                pageable
        );

        return new PageImpl<>(
                query.getResultList(),
                pageable,
                total(contatoFilter)
        );
    }

    @Override
    public List<Contato> filtrar(
            ContatoFilter contatoFilter) {

        CriteriaBuilder builder =
                manager.getCriteriaBuilder();

        CriteriaQuery<Contato> criteria =
                builder.createQuery(Contato.class);

        Root<Contato> root =
                criteria.from(Contato.class);

        root.fetch("pessoa", JoinType.INNER);
        root.fetch("dadosPessoaJuridica", JoinType.LEFT);

        Predicate[] predicates =
                criarRestricoes(contatoFilter, builder, root);

        criteria
                .select(root)
                .where(predicates);

        return manager
                .createQuery(criteria)
                .getResultList();
    }

    private Predicate[] criarRestricoes(
            ContatoFilter contatoFilter,
            CriteriaBuilder builder,
            Root<Contato> root) {

        List<Predicate> predicates =
                new ArrayList<>();

        if (contatoFilter.getId() != null) {
            predicates.add(
                    builder.equal(
                            root.get(Contato_.ID),
                            contatoFilter.getId()
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

    private Long total(ContatoFilter filter) {

        CriteriaBuilder builder =
                manager.getCriteriaBuilder();

        CriteriaQuery<Long> criteria =
                builder.createQuery(Long.class);

        Root<Contato> root =
                criteria.from(Contato.class);

        Predicate[] predicates =
                criarRestricoes(filter, builder, root);

        criteria
                .select(builder.count(root))
                .where(predicates);

        return manager
                .createQuery(criteria)
                .getSingleResult();
    }
}