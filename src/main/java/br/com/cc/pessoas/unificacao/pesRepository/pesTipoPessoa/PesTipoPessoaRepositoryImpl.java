package br.com.cc.pessoas.unificacao.pesRepository.pesTipoPessoa;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoDocumento_;
import br.com.cc.pessoas.unificacao.pesEntity.PesTipoPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.PesTipoPessoa_;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoPessoaFilter;
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

public class PesTipoPessoaRepositoryImpl implements PesTipoPessoaRepositoryQuery {
    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<PesTipoPessoa> filtrar(PesTipoPessoaFilter pesTipoPessoa, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesTipoPessoa> criteria = builder.createQuery(PesTipoPessoa.class);
        Root<PesTipoPessoa> root = criteria.from(PesTipoPessoa.class);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(pesTipoPessoa, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<PesTipoPessoa> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(pesTipoPessoa));

    }

    //Aqui da lista sem paginacao
    @Override
    public List<PesTipoPessoa> filtrar(PesTipoPessoaFilter pesTipoPessoa) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesTipoPessoa> criteria = builder.createQuery(PesTipoPessoa.class);
        Root<PesTipoPessoa> root = criteria.from(PesTipoPessoa.class);

        Predicate[] predicates = criarRestricoes(pesTipoPessoa, builder, root);
        criteria.where(predicates);

        TypedQuery<PesTipoPessoa> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesTipoPessoaFilter pesTipoPessoa, CriteriaBuilder builder, Root<PesTipoPessoa> root) {

        List<Predicate> predicates = new ArrayList<>();

        // POR TIPO PESSOA
        if(pesTipoPessoa.getTipoPessoa() != null) {
            predicates.add(builder.equal(root.get(PesTipoPessoa_.TIPO_PESSOA), pesTipoPessoa.getTipoPessoa()));
        }

        if (StringUtils.hasText(pesTipoPessoa.getDescricao())) {
            predicates.add(
                    builder.like(
                            builder.upper(root.get(PesTipoDocumento_.DESCRICAO)),
                            "%" + pesTipoPessoa.getDescricao().trim().toUpperCase() + "%"
                    )
            );
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

    private Long total(PesTipoPessoaFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesTipoPessoa> root = criteria.from(PesTipoPessoa.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}
