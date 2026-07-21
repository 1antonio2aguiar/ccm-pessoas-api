package br.com.cc.pessoas.repository.pessoaFisica;

import br.com.cc.pessoas.dto.pessoa.PessoaDTO;
import br.com.cc.pessoas.entity.DadosPessoaFisica;
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
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PessoaFisicaRepositoryImpl implements PessoaFisicaRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<PessoaDTO> filtrar(PessoaFilter filter, Pageable pageable) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();

        CriteriaQuery<DadosPessoaFisica> cq =
                cb.createQuery(DadosPessoaFisica.class);

        Root<DadosPessoaFisica> root =
                cq.from(DadosPessoaFisica.class);

        cq.select(root);

        Predicate[] predicates =
                criarRestricoes(filter, cb, root);

        cq.where(predicates);

        Join<DadosPessoaFisica, Pessoa> pessoaJoin =
                root.join("pessoa", JoinType.INNER);

        cq.orderBy(
                QueryUtils.toOrders(
                        pageable.getSort(),
                        pessoaJoin,
                        cb
                )
        );

        TypedQuery<DadosPessoaFisica> query =
                manager.createQuery(cq);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<PessoaDTO> lista =
                query.getResultList()
                        .stream()
                        .map(pf -> PessoaDTO.fromPessoa(pf.getPessoa()))
                        .toList();

        return new PageImpl<>(
                lista,
                pageable,
                total(filter)
        );
    }

    private Long total(PessoaFilter filter) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();

        CriteriaQuery<Long> cq =
                cb.createQuery(Long.class);

        Root<DadosPessoaFisica> root =
                cq.from(DadosPessoaFisica.class);

        cq.select(cb.count(root));

        cq.where(criarRestricoes(filter, cb, root));

        return manager.createQuery(cq)
                .getSingleResult();
    }

    private Predicate[] criarRestricoes(
            PessoaFilter filter,
            CriteriaBuilder cb,
            Root<DadosPessoaFisica> root) {

        List<Predicate> predicates = new ArrayList<>();

        Join<DadosPessoaFisica, Pessoa> pessoaJoin =
                root.join("pessoa", JoinType.INNER);

        if (filter.getId() != null) {
            predicates.add(
                    cb.equal(
                            pessoaJoin.get("id"),
                            filter.getId()
                    )
            );
        }

        if (StringUtils.hasText(filter.getNome())) {
            predicates.add(
                    cb.like(
                            cb.upper(pessoaJoin.get("nome")),
                            "%" + filter.getNome().trim().toUpperCase() + "%"
                    )
            );
        }

        if (StringUtils.hasText(filter.getCpf())
                && filter.getCpf().trim().length() >= 4) {

            String cpf = filter.getCpf()
                    .replaceAll("\\D", "")
                    .trim();

            predicates.add(
                    cb.like(
                            root.get("cpf"),
                            cpf + "%"
                    )
            );
        }

        if (filter.getDataNascimento() != null) {
            predicates.add(
                    cb.equal(
                            root.get("dataNascimento"),
                            filter.getDataNascimento()
                    )
            );
        }

        return predicates.toArray(new Predicate[0]);
    }

    @Override
    public List<PessoaDTO> filtrar(PessoaFilter filter) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();

        CriteriaQuery<DadosPessoaFisica> cq =
                cb.createQuery(DadosPessoaFisica.class);

        Root<DadosPessoaFisica> root =
                cq.from(DadosPessoaFisica.class);

        cq.select(root);
        cq.where(criarRestricoes(filter, cb, root));

        Join<DadosPessoaFisica, Pessoa> pessoaJoin =
                root.join("pessoa", JoinType.INNER);

        cq.orderBy(cb.asc(pessoaJoin.get("nome")));

        return manager.createQuery(cq)
                .getResultList()
                .stream()
                .map(pf -> PessoaDTO.fromPessoa(pf.getPessoa()))
                .toList();
    }
}