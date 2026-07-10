package br.com.cc.pessoas.unificacao.pesRepository.pesCpfNoNome;

import br.com.cc.pessoas.unificacao.pesDto.pesCpfNoNome.PesPessoaCpfNoNomeDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class PesPessoaCpfNoNomeRepositoryCustomImpl implements PesPessoaCpfNoNomeRepositoryCustom {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<PesPessoaCpfNoNomeDTO> listarCpfNoNome(String nome, Pageable pageable) {

        StringBuilder fromWhere = new StringBuilder();

        fromWhere.append("""
                from dbo_ccm_pessoas.pes_pessoas p
                where p.fisica_juridica = 'J'
                  and (
                        regexp_like(trim(p.nome), '^[0-9]{11}')
                     or regexp_like(trim(p.nome), '[0-9]{11}$')
                  )
                """);

        if (StringUtils.hasText(nome)) {
            fromWhere.append(" and upper(p.nome) like :nome ");
        }

        String sqlDados = """
                select
                    p.pessoa,
                    p.nome,
                    case
                        when regexp_like(trim(p.nome), '^[0-9]{11}') then
                            regexp_substr(trim(p.nome), '^[0-9]{11}')
                        when regexp_like(trim(p.nome), '[0-9]{11}$') then
                            regexp_substr(trim(p.nome), '[0-9]{11}$')
                    end as cpf_no_nome,
                    p.cgc_cpf,
                    case
                        when regexp_like(trim(p.nome), '^[0-9]{11}') then
                            trim(regexp_replace(trim(p.nome), '^[0-9]{11}[[:space:]]*', ''))
                        when regexp_like(trim(p.nome), '[0-9]{11}$') then
                            trim(regexp_replace(trim(p.nome), '[[:space:]]*[0-9]{11}$', ''))
                    end as nome_sem_cpf,
                    case
                        when regexp_like(trim(p.nome), '^[0-9]{11}') then 'INICIO'
                        when regexp_like(trim(p.nome), '[0-9]{11}$') then 'FINAL'
                    end as posicao_cpf
                """ + fromWhere + """
                order by p.nome
                """;

        String sqlCount = "select count(*) " + fromWhere;

        Query queryDados = manager.createNativeQuery(sqlDados);
        Query queryCount = manager.createNativeQuery(sqlCount);

        aplicarParametros(nome, queryDados);
        aplicarParametros(nome, queryCount);

        queryDados.setFirstResult((int) pageable.getOffset());
        queryDados.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = queryDados.getResultList();

        Long total = ((Number) queryCount.getSingleResult()).longValue();

        List<PesPessoaCpfNoNomeDTO> lista = rows.stream()
                .map(this::toDto)
                .toList();

        return new PageImpl<>(lista, pageable, total);
    }

    private void aplicarParametros(String nome, Query query) {
        if (StringUtils.hasText(nome)) {
            query.setParameter("nome", "%" + nome.trim().toUpperCase() + "%");
        }
    }

    private PesPessoaCpfNoNomeDTO toDto(Object[] r) {
        return new PesPessoaCpfNoNomeDTO(
                num(r[0]),
                str(r[1]),
                str(r[2]),
                num(r[3]),
                str(r[4]),
                str(r[5])
        );
    }

    private Long num(Object o) {
        if (o == null) {
            return null;
        }

        if (o instanceof Number n) {
            return n.longValue();
        }

        String s = String.valueOf(o).trim();

        if (s.isEmpty()) {
            return null;
        }

        return Long.valueOf(s);
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}