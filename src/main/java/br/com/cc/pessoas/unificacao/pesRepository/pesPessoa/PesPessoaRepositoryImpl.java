package br.com.cc.pessoas.unificacao.pesRepository.pesPessoa;

import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa_;
import br.com.cc.pessoas.unificacao.pesFilter.PesPessoaFilter;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PesPessoaRepositoryImpl implements PesPessoaRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<PesPessoa> filtrar(PesPessoaFilter pesPessoa, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesPessoa> criteria = builder.createQuery(PesPessoa.class);
        Root<PesPessoa> root = criteria.from(PesPessoa.class);

        root.fetch("pesCidade", JoinType.LEFT);
        root.fetch("pesDistrito", JoinType.LEFT);
        root.fetch("pesBairro", JoinType.LEFT);
        root.fetch("pesLogradouro", JoinType.LEFT);
        root.fetch("pesTipoPessoa", JoinType.LEFT);
        root.fetch("pesTipoDocumento", JoinType.LEFT);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(pesPessoa, builder, root);
        criteria.where(predicates).orderBy(orders).distinct(true);

        TypedQuery<PesPessoa> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(pesPessoa));
    }

    @Override
    public List<PesPessoa> filtrar(PesPessoaFilter pesPessoa) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<PesPessoa> criteria = builder.createQuery(PesPessoa.class);
        Root<PesPessoa> root = criteria.from(PesPessoa.class);

        root.fetch("pesCidade", JoinType.LEFT);
        root.fetch("pesDistrito", JoinType.LEFT);
        root.fetch("pesBairro", JoinType.LEFT);
        root.fetch("pesLogradouro", JoinType.LEFT);
        root.fetch("pesTipoPessoa", JoinType.LEFT);
        root.fetch("pesTipoDocumento", JoinType.LEFT);

        Predicate[] predicates = criarRestricoes(pesPessoa, builder, root);
        criteria.where(predicates).distinct(true);

        TypedQuery<PesPessoa> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
            PesPessoaFilter pesPessoa, CriteriaBuilder builder, Root<PesPessoa> root) {

        List<Predicate> predicates = new ArrayList<>();

        if (pesPessoa.getPessoa() != null) {
            predicates.add(builder.equal(root.get(PesPessoa_.PESSOA), pesPessoa.getPessoa()));
        }

        if (StringUtils.hasText(pesPessoa.getNome())) {
            predicates.add(
                    builder.like(
                            builder.upper(root.get(PesPessoa_.NOME)),
                            "%" + pesPessoa.getNome().trim().toUpperCase() + "%"
                    )
            );
        }

        if (pesPessoa.getCpf() != null) {
            predicates.add(builder.equal(root.get(PesPessoa_.CGC_CPF), pesPessoa.getCpf()));
            predicates.add(builder.equal(root.get(PesPessoa_.FISICA_JURIDICA), "F"));
        }

        if (pesPessoa.getCnpj() != null) {
            predicates.add(builder.equal(root.get(PesPessoa_.CGC_CPF), pesPessoa.getCnpj()));
            predicates.add(builder.equal(root.get(PesPessoa_.FISICA_JURIDICA), "J"));
        }

        if (pesPessoa.getDataNascimento() != null) {
            LocalDateTime ini = pesPessoa.getDataNascimento().atStartOfDay();
            LocalDateTime fim = pesPessoa.getDataNascimento().plusDays(1).atStartOfDay();

            predicates.add(builder.greaterThanOrEqualTo(root.get(PesPessoa_.DATA_NASCIMENTO), ini));
            predicates.add(builder.lessThan(root.get(PesPessoa_.DATA_NASCIMENTO), fim));
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

    private Long total(PesPessoaFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<PesPessoa> root = criteria.from(PesPessoa.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}