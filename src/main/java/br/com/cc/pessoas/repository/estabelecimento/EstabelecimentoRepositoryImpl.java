package br.com.cc.pessoas.repository.estabelecimento;

import br.com.cc.pessoas.dto.estabelecimento.EstabelecimentoDTO;
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
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
public class EstabelecimentoRepositoryImpl
        implements EstabelecimentoRepositoryQuery {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<EstabelecimentoDTO> filtrar(
            PessoaFilter filter,
            Pageable pageable) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();

        CriteriaQuery<DadosPessoaJuridica> cq =
                cb.createQuery(DadosPessoaJuridica.class);

        Root<DadosPessoaJuridica> root =
                cq.from(DadosPessoaJuridica.class);

        cq.select(root);

        Predicate[] predicates =
                criarRestricoes(filter, cb, root);

        cq.where(predicates);

        cq.orderBy(
                QueryUtils.toOrders(
                        pageable.getSort(),
                        root,
                        cb
                )
        );

        TypedQuery<DadosPessoaJuridica> query =
                manager.createQuery(cq);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<EstabelecimentoDTO> lista =
                query.getResultList()
                        .stream()
                        .map(EstabelecimentoDTO::fromEntity)
                        .toList();

        return new PageImpl<>(
                lista,
                pageable,
                total(filter)
        );
    }

    @Override
    public List<EstabelecimentoDTO> filtrar(
            PessoaFilter filter) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();

        CriteriaQuery<DadosPessoaJuridica> cq =
                cb.createQuery(DadosPessoaJuridica.class);

        Root<DadosPessoaJuridica> root =
                cq.from(DadosPessoaJuridica.class);

        cq.select(root);

        cq.where(
                criarRestricoes(filter, cb, root)
        );

        cq.orderBy(
                cb.asc(root.get("nome"))
        );

        return manager.createQuery(cq)
                .getResultList()
                .stream()
                .map(EstabelecimentoDTO::fromEntity)
                .toList();
    }

    private Long total(PessoaFilter filter) {

        CriteriaBuilder cb = manager.getCriteriaBuilder();

        CriteriaQuery<Long> cq =
                cb.createQuery(Long.class);

        Root<DadosPessoaJuridica> root =
                cq.from(DadosPessoaJuridica.class);

        cq.select(cb.count(root));

        cq.where(
                criarRestricoes(filter, cb, root)
        );

        return manager.createQuery(cq)
                .getSingleResult();
    }

    private Predicate[] criarRestricoes(
            PessoaFilter filter,
            CriteriaBuilder cb,
            Root<DadosPessoaJuridica> root) {

        List<Predicate> predicates = new ArrayList<>();

        Join<DadosPessoaJuridica, Pessoa> pessoaJoin =
                root.join("pessoa", JoinType.INNER);

        Join<Pessoa, DadosPessoaFisica> pessoaFisicaJoin =
                pessoaJoin.join(
                        "dadosPessoaFisica",
                        JoinType.LEFT
                );

        if (filter.getId() != null) {
            predicates.add(
                    cb.equal(
                            root.get("id"),
                            filter.getId()
                    )
            );
        }

        if (StringUtils.hasText(filter.getNome())) {
            predicates.add(
                    cb.like(
                            cb.upper(root.get("nome")),
                            "%" + filter.getNome()
                                    .trim()
                                    .toUpperCase() + "%"
                    )
            );
        }

        if (StringUtils.hasText(filter.getNomeProprietario())) {
            predicates.add(
                    cb.like(
                            cb.upper(pessoaJoin.get("nome")),
                            "%" + filter.getNomeProprietario()
                                    .trim()
                                    .toUpperCase() + "%"
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
                            pessoaFisicaJoin.get("cpf"),
                            cpf + "%"
                    )
            );
        }

        if (StringUtils.hasText(filter.getCnpj())
                && filter.getCnpj().trim().length() >= 4) {

            String cnpj = filter.getCnpj()
                    .replaceAll("\\D", "")
                    .trim();

            predicates.add(
                    cb.like(
                            root.get("cnpj"),
                            cnpj + "%"
                    )
            );
        }

        return predicates.toArray(new Predicate[0]);
    }
}