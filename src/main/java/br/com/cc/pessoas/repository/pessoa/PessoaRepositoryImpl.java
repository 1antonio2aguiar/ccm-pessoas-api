package br.com.cc.pessoas.repository.pessoa;

import br.com.cc.pessoas.entity.DadosPessoaFisica;
import br.com.cc.pessoas.entity.DadosPessoaJuridica;
import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.filter.PessoaFilter;
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

public class PessoaRepositoryImpl implements PessoaRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public List<Pessoa> filtrar(PessoaFilter filter) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Pessoa> cq = cb.createQuery(Pessoa.class);
        Root<Pessoa> root = cq.from(Pessoa.class);

        cq.select(root).distinct(true);
        cq.where(criarRestricoes(filter, cb, root));

        return manager.createQuery(cq).getResultList();
    }

    @Override
    public Page<Pessoa> filtrar(PessoaFilter filter, Pageable pageable) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Pessoa> cq = cb.createQuery(Pessoa.class);
        Root<Pessoa> root = cq.from(Pessoa.class);

        cq.select(root).distinct(true);
        cq.where(criarRestricoes(filter, cb, root));
        cq.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));

        TypedQuery<Pessoa> query = manager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(
                query.getResultList(),
                pageable,
                total(filter)
        );
    }

    private Predicate[] criarRestricoes(
            PessoaFilter filter,
            CriteriaBuilder cb,
            Root<Pessoa> root) {

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getId() != null) {
            predicates.add(
                    cb.equal(root.get("id"), filter.getId())
            );
        }

        if (StringUtils.hasText(filter.getNome())) {
            predicates.add(
                    cb.like(
                            cb.upper(root.get("nome")),
                            "%" + filter.getNome().trim().toUpperCase() + "%"
                    )
            );
        }

        if (StringUtils.hasText(filter.getCpf())
                && filter.getCpf().trim().length() >= 4) {

            Join<Pessoa, DadosPessoaFisica> pf =
                    root.join("dadosPessoaFisica", JoinType.INNER);

            predicates.add(
                    cb.like(
                            pf.get("cpf"),
                            filter.getCpf().trim() + "%"
                    )
            );
        }

        if (StringUtils.hasText(filter.getCnpj())) {

            Join<Pessoa, DadosPessoaJuridica> pj =
                    root.join("dadosPessoasJuridicas", JoinType.INNER);

            predicates.add(
                    cb.like(
                            pj.get("cnpj"),
                            filter.getCnpj().trim()
                    )
            );
        }

        if (filter.getDataNascimento() != null) {

            Join<Pessoa, DadosPessoaFisica> pf =
                    root.join("dadosPessoaFisica", JoinType.INNER);

            predicates.add(
                    cb.equal(
                            pf.get("dataNascimento"),
                            filter.getDataNascimento()
                    )
            );
        }

        return predicates.toArray(new Predicate[0]);
    }

    private Long total(PessoaFilter filter) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Pessoa> root = cq.from(Pessoa.class);

        cq.select(cb.countDistinct(root));
        cq.where(criarRestricoes(filter, cb, root));

        return manager.createQuery(cq).getSingleResult();
    }

    @Override
    public boolean existeCpfCnpjNoCadUnico(
            Long cpfCnpj,
            String fisicaJuridica) {

        if (cpfCnpj == null || cpfCnpj == 0L) {
            return false;
        }

        String documento = String.valueOf(cpfCnpj)
                .replaceAll("\\D", "");

        if (documento.length() <= 11) {

            String cpf = String.format("%011d", cpfCnpj);

            Long quantidade = ((Number) manager.createNativeQuery("""
                    select count(1)
                      from DBO_CCM_PESSOAS.DADOS_PF dpf
                     where dpf.CPF = :cpf
                    """)
                    .setParameter("cpf", cpf)
                    .getSingleResult())
                    .longValue();

            return quantidade > 0;
        }

        String cnpj = String.format("%014d", cpfCnpj);

        Long quantidade = ((Number) manager.createNativeQuery("""
                select count(1)
                  from DBO_CCM_PESSOAS.DADOS_PJ dpj
                 where dpj.CNPJ = :cnpj
                """)
                .setParameter("cnpj", cnpj)
                .getSingleResult())
                .longValue();

        return quantidade > 0;
    }
}