package br.com.cc.pessoas.repository.estado;

import br.com.cc.pessoas.entity.Estado;
import br.com.cc.pessoas.filter.EstadoFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class EstadoRepositoryImpl implements EstadoRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public List<Estado> filtrar(EstadoFilter filter) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Estado> criteria = builder.createQuery(Estado.class);
        Root<Estado> root = criteria.from(Estado.class);

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getId() != null) {
            predicates.add(builder.equal(root.get("id"), filter.getId()));
        }

        if (filter.getPaisId() != null) {
            predicates.add(builder.equal(root.get("pais").get("id"), filter.getPaisId()));
        }

        if (StringUtils.hasText(filter.getNome())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get("nome")),
                            "%" + filter.getNome().toLowerCase() + "%"
                    )
            );
        }

        criteria.where(predicates.toArray(new Predicate[0]));

        return manager.createQuery(criteria).getResultList();
    }
}
