package br.com.cc.pessoas.unificacao.dashboard.repository;

import br.com.cc.pessoas.unificacao.dashboard.dto.CadUnicoDashboardResumoDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class CadUnicoDashboardRepository {

    @PersistenceContext
    private EntityManager manager;

    public CadUnicoDashboardResumoDTO buscarResumo() {
        return new CadUnicoDashboardResumoDTO(
                count("""
                    select count(*)
                      from dbo_ccm_pessoas.pessoas p
                """),

                count("""
                    select count(*)
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                """),

                count("""
                    select count(*)
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                     where cup.banco = 'PESSOAS'
                """),

                count("""
                    select count(*)
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                     where cup.banco = 'RH'
                """),

                count("""
                    select count(*)
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                     where cup.banco = 'SANE'
                """),

                count("""
                    select count(*)
                      from dbo_ccm_pessoas.pessoas p
                     where p.fisica_juridica = 'F'
                """),

                count("""
                    select count(*)
                      from dbo_ccm_pessoas.pessoas p
                     where p.fisica_juridica = 'J'
                """),

                count("""
                    select count(*)
                      from dbo_ccm_pessoas.pessoas p
                     where not exists (
                            select 1
                              from dbo_ccm_pessoas.enderecos e
                             where e.pessoa_id = p.id
                     )
                """),

                count("""
                    select count(*)
                      from (
                            select cup.pessoas_cd_unico
                              from dbo_ccm_pessoas.cad_unico_pessoa cup
                             where cup.pessoas_cd_unico is not null
                             group by cup.pessoas_cd_unico
                            having count(*) > 1
                           )
                """)
        );
    }

    private Long count(String sql) {
        Object result = manager.createNativeQuery(sql).getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }
}