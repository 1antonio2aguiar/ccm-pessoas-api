package br.com.cc.pessoas.repository.pessoa;

import br.com.cc.pessoas.dto.pessoa.PessoaCpfCnpjCadUnicoDTO;
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

import java.time.LocalDateTime;
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
    @SuppressWarnings("unchecked")
    public PessoaCpfCnpjCadUnicoDTO existeCpfCnpjNoCadUnico(
            Long cpfCnpj,
            String fisicaJuridica) {

        if (cpfCnpj == null || cpfCnpj == 0L) {
            return new PessoaCpfCnpjCadUnicoDTO(
                    false,
                    null,
                    null,
                    null,
                    null
            );
        }

        List<Object[]> resultado;

        if ("F".equalsIgnoreCase(fisicaJuridica)) {

            String cpf = String.format(
                    "%011d",
                    cpfCnpj
            );

            resultado = manager.createNativeQuery("""
            select
                p.id,
                p.nome,
                dpf.cpf,
                dpf.data_nascimento
              from DBO_CCM_PESSOAS.DADOS_PF dpf
              join DBO_CCM_PESSOAS.PESSOAS p
                on p.id = dpf.id
             where dpf.cpf = :cpf
               and rownum = 1
        """)
                    .setParameter(
                            "cpf",
                            cpf
                    )
                    .getResultList();

        } else if ("J".equalsIgnoreCase(fisicaJuridica)) {

            String cnpj = String.format(
                    "%014d",
                    cpfCnpj
            );

            resultado = manager.createNativeQuery("""
            select
                p.id,
                p.nome,
                dpj.cnpj,
                cast(null as timestamp) as data_nascimento
              from DBO_CCM_PESSOAS.DADOS_PJ dpj
              join DBO_CCM_PESSOAS.PESSOAS p
                on p.id = dpj.pessoa_id
             where dpj.cnpj = :cnpj
               and rownum = 1
        """)
                    .setParameter(
                            "cnpj",
                            cnpj
                    )
                    .getResultList();

        } else {

            return new PessoaCpfCnpjCadUnicoDTO(
                    false,
                    null,
                    null,
                    null,
                    null
            );
        }

        if (resultado.isEmpty()) {
            return new PessoaCpfCnpjCadUnicoDTO(
                    false,
                    null,
                    null,
                    null,
                    null
            );
        }

        Object[] registro = resultado.get(0);

        Long pessoaId =
                registro[0] == null
                        ? null
                        : ((Number) registro[0]).longValue();

        String nome =
                registro[1] == null
                        ? null
                        : registro[1].toString();

        String documento =
                registro[2] == null
                        ? null
                        : registro[2].toString();

        LocalDateTime dataNascimento = null;

        if (registro[3] instanceof java.sql.Timestamp timestamp) {
            dataNascimento =
                    timestamp.toLocalDateTime();
        }

        if (registro[3] instanceof java.sql.Date data) {
            dataNascimento =
                    data.toLocalDate()
                            .atStartOfDay();
        }

        return new PessoaCpfCnpjCadUnicoDTO(
                true,
                pessoaId,
                nome,
                documento,
                dataNascimento
        );
    }
}