package br.com.cc.pessoas.unificacao.pesRepository.cadUnicoPessoa;

import br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa.CadUnicoEnderecoOrigemDTO;
import br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa.CadUnicoPessoaOrigemDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CadUnicoPessoaRepositoryCustomImpl implements CadUnicoPessoaRepositoryCustom {

    @PersistenceContext
    private EntityManager manager;

    @Override
    @SuppressWarnings("unchecked")
    public List<CadUnicoPessoaOrigemDTO> buscarOrigens(Long pessoasCdUnico) {

        List<Object[]> rows = manager.createNativeQuery("""
        select
            cup.id,
            cup.cd_origem,
            cup.tipo_pessoa,
            cup.nome,
            cup.fisica_juridica,
            cup.cpf_cnpj,
            cup.estado_civil,
            cup.sexo,
            cup.email,
            cup.banco,
            cup.pessoas_cd_unico,
            cup.status,
            cup.data_nascimento,
            cup.data_cadastro,
            cup.observacao
        from dbo_ccm_pessoas.cad_unico_pessoa cup
        where cup.pessoas_cd_unico = :pessoasCdUnico
        order by cup.banco, cup.nome, cup.id
    """)
                .setParameter("pessoasCdUnico", pessoasCdUnico)
                .getResultList();

        List<CadUnicoPessoaOrigemDTO> lista = new ArrayList<>();

        for (Object[] r : rows) {
            Long cdOrigem = num(r[1]);
            String banco = str(r[9]);

            String cidadeNascimentoNome = buscarCidadeNascimentoOrigem(banco, cdOrigem);

            lista.add(new CadUnicoPessoaOrigemDTO(
                    num(r[0]),
                    cdOrigem,
                    num(r[2]),
                    str(r[3]),
                    str(r[4]),
                    num(r[5]),
                    str(r[6]),
                    str(r[7]),
                    str(r[8]),
                    banco,
                    num(r[10]),
                    str(r[11]),
                    ldt(r[12]),
                    ldt(r[13]),
                    str(r[14]),
                    cidadeNascimentoNome,
                    buscarEnderecosOrigem(banco, cdOrigem)
            ));
        }

        return lista;
    }

    private String buscarCidadeNascimentoOrigem(String banco, Long cdOrigem) {
        if (banco == null || cdOrigem == null) {
            return null;
        }

        if ("PESSOAS".equalsIgnoreCase(banco)) {
            return buscarCidadeNascimentoPessoas(cdOrigem);
        }

        if ("RH".equalsIgnoreCase(banco)) {
            return buscarCidadeNascimentoRh(cdOrigem);
        }

        return null;
    }

    private String buscarCidadeNascimentoPessoas(Long cdOrigem) {
        try {
            Object result = manager.createNativeQuery("""
            select pd.nome || ' - ' || pe.estado
              from dbo_ccm_pessoas.cad_unico_pessoa cup
              join dbo_ccm_pessoas.pes_pessoas pp
                on pp.pessoa = cup.cd_origem
              join dbo_ccm_pessoas.pes_distritos pd
                on pd.cidade = pp.cidade
               and pd.distrito = pp.distrito
              join dbo_ccm_pessoas.pes_cidades pc
                on pc.cidade = pp.cidade
              join dbo_ccm_pessoas.pes_estados pe
                on pe.estado = pc.estado
             where cup.cd_origem = :cdOrigem
               and cup.banco = 'PESSOAS'
               and rownum = 1
        """)
                    .setParameter("cdOrigem", cdOrigem)
                    .getSingleResult();

            return result == null ? null : String.valueOf(result);

        } catch (Exception e) {
            return null;
        }
    }
    private String buscarCidadeNascimentoRh(Long cdOrigem) {
        try {
            Object result = manager.createNativeQuery("""
            select rd.nome || ' - ' || re.estado
              from dbo_ccm_pessoas.cad_unico_pessoa cup
              join dbo_ccm_pessoas.rh_pessoas rp
                on rp.pessoa = cup.cd_origem
              join dbo_ccm_pessoas.rh_distritos rd
                on rd.cidade = rp.cidade
               and rd.distrito = rp.distrito
              join dbo_ccm_pessoas.rh_cidades rc
                on rc.cidade = rp.cidade
              join dbo_ccm_pessoas.rh_estados re
                on re.estado = rc.estado
             where cup.cd_origem = :cdOrigem
               and cup.banco = 'RH'
               and rownum = 1
        """)
                    .setParameter("cdOrigem", cdOrigem)
                    .getSingleResult();

            return result == null ? null : String.valueOf(result);

        } catch (Exception e) {
            return null;
        }
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

    private LocalDateTime ldt(Object o) {
        if (o == null) {
            return null;
        }

        if (o instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }

        return null;
    }

    private List<CadUnicoEnderecoOrigemDTO> buscarEnderecosOrigem(String banco, Long cdOrigem) {
        if (banco == null || cdOrigem == null) {
            return new ArrayList<>();
        }

        if ("PESSOAS".equalsIgnoreCase(banco)) {
            return buscarEnderecosOrigemPessoas(cdOrigem);
        }

        if ("RH".equalsIgnoreCase(banco)) {
            return buscarEnderecosOrigemRh(cdOrigem);
        }

        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<CadUnicoEnderecoOrigemDTO> buscarEnderecosOrigemPessoas(Long cdOrigem) {
        List<Object[]> rows = manager.createNativeQuery("""
        select
            'PESSOAS' as banco,
            cup.cd_origem,
            cid.cidade,
            cid.nome cidade_nome,
            dis.distrito,
            dis.nome distrito_nome,
            bai.bairro,
            bai.nome bairro_nome,
            log.logradouro,
            log.nome logradouro_nome,
            pes.numero,
            pes.complemento,
            pes.cep,
            est.estado,
            log.tipo_logradouro
        from dbo_ccm_pessoas.cad_unico_pessoa cup
        join dbo_ccm_pessoas.pes_pessoas pes
          on pes.pessoa = cup.cd_origem
        join dbo_ccm_pessoas.pes_cidades cid
          on cid.cidade = pes.cidade
        join dbo_ccm_pessoas.pes_estados est
          on est.estado = cid.estado
        join dbo_ccm_pessoas.pes_distritos dis
          on dis.cidade = pes.cidade
         and dis.distrito = pes.distrito
        left join dbo_ccm_pessoas.pes_bairros bai
          on bai.cidade = pes.cidade
         and bai.distrito = pes.distrito
         and bai.bairro = pes.bairro
        left join dbo_ccm_pessoas.pes_logradouros log
          on log.cidade = pes.cidade
         and log.distrito = pes.distrito
         and log.logradouro = pes.logradouro
        where cup.cd_origem = :cdOrigem
          and cup.banco = 'PESSOAS'
    """)
                .setParameter("cdOrigem", cdOrigem)
                .getResultList();

        return rows.stream()
                .map(this::toEnderecoOrigemDto)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<CadUnicoEnderecoOrigemDTO> buscarEnderecosOrigemRh(Long cdOrigem) {
        List<Object[]> rows = manager.createNativeQuery("""
        select
            'RH' as banco,
            cup.cd_origem,
            cid.cidade,
            cid.nome cidade_nome,
            dis.distrito,
            dis.nome distrito_nome,
            bai.bairro,
            bai.nome bairro_nome,
            log.logradouro,
            log.nome logradouro_nome,
            pes.numero,
            pes.complemento,
            pes.cep,
            est.estado,
            log.tipo_logradouro
        from dbo_ccm_pessoas.cad_unico_pessoa cup
        join dbo_ccm_pessoas.rh_pessoas pes
          on pes.pessoa = cup.cd_origem
        join dbo_ccm_pessoas.rh_cidades cid
          on cid.cidade = pes.cidade
        join dbo_ccm_pessoas.rh_estados est
          on est.estado = cid.estado
        join dbo_ccm_pessoas.rh_distritos dis
          on dis.cidade = pes.cidade
         and dis.distrito = pes.distrito
        left join dbo_ccm_pessoas.rh_bairros bai
          on bai.cidade = pes.cidade
         and bai.distrito = pes.distrito
         and bai.bairro = pes.bairro
        left join dbo_ccm_pessoas.rh_logradouros log
          on log.cidade = pes.cidade
         and log.distrito = pes.distrito
         and log.logradouro = pes.logradouro
        where cup.cd_origem = :cdOrigem
          and cup.banco = 'RH'
    """)
                .setParameter("cdOrigem", cdOrigem)
                .getResultList();

        return rows.stream()
                .map(this::toEnderecoOrigemDto)
                .toList();
    }

    private CadUnicoEnderecoOrigemDTO toEnderecoOrigemDto(Object[] r) {
        return new CadUnicoEnderecoOrigemDTO(
                str(r[0]),   // banco
                num(r[1]),   // cdOrigem
                num(r[2]),   // cidade
                str(r[3]),   // cidadeNome
                num(r[4]),   // distrito
                str(r[5]),   // distritoNome
                num(r[6]),   // bairro
                str(r[7]),   // bairroNome
                num(r[8]),   // logradouro
                str(r[9]),   // logradouroNome
                num(r[10]),  // numero
                str(r[11]),  // complemento
                num(r[12]),  // cep
                str(r[13]),  // uf
                str(r[14])   // tipoLogradouro
        );
    }


}