package br.com.cc.pessoas.repository.endereco;

import br.com.cc.pessoas.entity.*;
import com.pesoas.api.filter.enderecos.EnderecoFilter;
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

public class EnderecoRepositoryImpl implements EnderecoRepositoryQuery {
    @PersistenceContext
    private EntityManager manager;
    @Override
    public Page<Endereco> filtrar(EnderecoFilter enderecoFilter, Pageable pageable) {

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Endereco> criteria = builder.createQuery(Endereco.class);
        Root<Endereco> root = criteria.from(Endereco.class);

        List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, builder);

        Predicate[] predicates = criarRestricoes(enderecoFilter, builder, root);
        criteria.where(predicates).orderBy(orders);

        TypedQuery<Endereco> query = manager.createQuery(criteria);
        adicionarRestricoesDePaginacao(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, total(enderecoFilter));

    }

    //Aqui da lista sem paginacao
    @Override
    public List<Endereco> filtrar(EnderecoFilter enderecoFilter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Endereco> criteria = builder.createQuery(Endereco.class);
        Root<Endereco> root = criteria.from(Endereco.class);

        Predicate[] predicates = criarRestricoes(enderecoFilter, builder, root);
        criteria.where(predicates);

        TypedQuery<Endereco> query = manager.createQuery(criteria);
        return query.getResultList();
    }

    private Predicate[] criarRestricoes(
        EnderecoFilter enderecoFilter, CriteriaBuilder builder, Root<Endereco> root) {

        List<Predicate> predicates = new ArrayList<>();

        // ID
        /*if(enderecoFilter.getId() != null) {
            predicates.add(builder.equal(root.get(Endereco_.ID), enderecoFilter.getId()));
        }*/

        // CEP
        /*if(StringUtils.hasLength(enderecoFilter.getCep())) {
            predicates.add(builder.equal(root.get(Endereco_.CEP).get(Cep_.CEP), enderecoFilter.getCep()));
        }*/

        // NOME DO LOGRADOURO
        /*if (StringUtils.hasLength(enderecoFilter.getLogradouro())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get(Endereco_.LOGRADOURO).get(Logradouro_.NOME)),
                            "%" + enderecoFilter.getLogradouro().toLowerCase() + "%"));
        }*/

        /*if (StringUtils.hasLength(enderecoFilter.getPessoa())) {
            predicates.add(
                    builder.like(
                            builder.lower(root.get(Endereco_.PESSOA).get(Pessoa_.NOME)), // Caminho corrigido!
                            "%" + enderecoFilter.getPessoa().toLowerCase() + "%"));
        }*/

        return predicates.toArray(new Predicate[predicates.size()]);
    }
    private void adicionarRestricoesDePaginacao(TypedQuery<?> query, Pageable pageable) {
        int paginaAtual = pageable.getPageNumber();
        int totalRegistrosPorPagina = pageable.getPageSize();
        int primeiroRegistroDaPagina = paginaAtual * totalRegistrosPorPagina;

        query.setFirstResult(primeiroRegistroDaPagina);
        query.setMaxResults(totalRegistrosPorPagina);
    }

    private Long total(com.pesoas.api.filter.enderecos.EnderecoFilter filter) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Endereco> root = criteria.from(Endereco.class);

        Predicate[] predicates = criarRestricoes(filter, builder, root);
        criteria.where(predicates);

        criteria.select(builder.count(root));
        return manager.createQuery(criteria).getSingleResult();
    }
}