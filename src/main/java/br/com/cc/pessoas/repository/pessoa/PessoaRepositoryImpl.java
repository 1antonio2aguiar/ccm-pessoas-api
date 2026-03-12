package br.com.cc.pessoas.repository.pessoa;

import br.com.cc.pessoas.entity.*;
import br.com.cc.pessoas.filter.PessoaFilter;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PessoaRepositoryImpl implements PessoaRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public List<Pessoa> filtrar(PessoaFilter filter) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Pessoa> cq = cb.createQuery(Pessoa.class);
        Root<Pessoa> root = cq.from(Pessoa.class);

        cq.where(criarRestricoes(filter, cb, root));

        return manager.createQuery(cq).getResultList();
    }

    @Override
    public Page<Pessoa> filtrar(PessoaFilter filter, Pageable pageable) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Pessoa> cq = cb.createQuery(Pessoa.class);
        Root<Pessoa> root = cq.from(Pessoa.class);

        cq.where(criarRestricoes(filter, cb, root));
        cq.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));

        TypedQuery<Pessoa> query = manager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(query.getResultList(), pageable, total(filter));
    }

    private Predicate[] criarRestricoes(PessoaFilter filter,
                                        CriteriaBuilder cb,
                                        Root<Pessoa> root) {

        List<Predicate> predicates = new ArrayList<>();

        // 🔹 ID
        if (filter.getId() != null) {
            predicates.add(cb.equal(root.get(Pessoa_.ID), filter.getId()));
        }

        // 🔹 NOME (LIKE ignore case)
        if (StringUtils.hasText(filter.getNome())) {
            predicates.add(
                    cb.like(
                            cb.upper(root.get(Pessoa_.NOME)),
                            "%" + filter.getNome().trim().toUpperCase() + "%"
                    )
            );
        }

        // 🔹 CPF (Pessoa Física)
        if (StringUtils.hasText(filter.getCpf())
                && filter.getCpf().trim().length() >= 4) {

            Root<DadosPessoaFisica> pf = cb.treat(root, DadosPessoaFisica.class);

            predicates.add(
                cb.like(
                        pf.get(DadosPessoaFisica_.CPF),
                        filter.getCpf().trim() + "%"
                )
            );
        }

        // 🔹 CNPJ (Pessoa Jurídica)
        if (StringUtils.hasText(filter.getCnpj())) {

            Root<DadosPessoaJuridica> pj = cb.treat(root, DadosPessoaJuridica.class);

            predicates.add(
            cb.like(
                    pj.get(DadosPessoaJuridica_.CNPJ),
                    filter.getCnpj().trim()
            )
            );
        }

        if (filter.getDataNascimento() != null) {

            Root<DadosPessoaFisica> pf = cb.treat(root, DadosPessoaFisica.class);

            var ini = filter.getDataNascimento().atStartOfDay();           // 1968-09-14 00:00
            var fim = filter.getDataNascimento().plusDays(1).atStartOfDay(); // 1968-09-15 00:00

            predicates.add(cb.greaterThanOrEqualTo(
                    pf.get(DadosPessoaFisica_.DATA_NASCIMENTO),
                    java.sql.Timestamp.valueOf(ini)
            ));

            predicates.add(cb.lessThan(
                    pf.get(DadosPessoaFisica_.DATA_NASCIMENTO),
                    java.sql.Timestamp.valueOf(fim)
            ));
        }

        return predicates.toArray(new Predicate[0]);
    }

    private Long total(PessoaFilter filter) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Pessoa> root = cq.from(Pessoa.class);

        cq.select(cb.count(root));
        cq.where(criarRestricoes(filter, cb, root));

        return manager.createQuery(cq).getSingleResult();
    }
}
