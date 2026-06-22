package br.com.cc.pessoas.unificacao.saneamento.saneService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SaneCargaPessoaHelperService {

    @PersistenceContext
    private EntityManager manager;

    public Long getNextVal(String sequence) {
        return ((Number) manager
                .createNativeQuery("select " + sequence + ".nextval from dual")
                .getSingleResult()).longValue();
    }

    public Long count(String sql, String paramName, Object paramValue) {
        return ((Number) manager.createNativeQuery(sql)
                .setParameter(paramName, paramValue)
                .getSingleResult()).longValue();
    }

    public String normalizarCpf(Long cgcCpf) {
        if (cgcCpf == null) return null;

        String cpf = String.valueOf(cgcCpf).trim();

        if (cpf.length() < 11) {
            return "0".repeat(11 - cpf.length()) + cpf;
        }

        if (cpf.length() > 11) {
            return cpf.substring(0, 11);
        }

        return cpf;
    }

    public Long converterTipoPessoa(Long tipoPessoaOrigem) {
        if (tipoPessoaOrigem == null) return null;

        if (tipoPessoaOrigem == 7L) return 24L;
        if (tipoPessoaOrigem == 8L) return 22L;
        if (tipoPessoaOrigem == 9L) return 23L;

        return tipoPessoaOrigem;
    }

    public Integer mapearEstadoCivil(String estadoCivil) {
        if (estadoCivil == null || estadoCivil.isBlank()) return null;

        return switch (estadoCivil.trim()) {
            case "A" -> 0;
            case "C" -> 1;
            case "D" -> 2;
            case "O" -> 6;
            case "S" -> 3;
            case "U" -> 4;
            case "V" -> 5;
            default -> null;
        };
    }

    public Integer mapearTipoDocumento(Integer tipoDocumento) {
        if (tipoDocumento == null) return null;

        return switch (tipoDocumento) {
            case 1 -> 5;
            case 2 -> 0;
            case 3 -> 1;
            case 4 -> 4;
            case 6 -> 3;
            case 7, 8, 9, 15 -> 10;
            case 99 -> null;
            default -> null;
        };
    }

    public Long buscarCidadeNascimentoCcmSane(Long cidadeNascimentoSane) {
        if (cidadeNascimentoSane == null) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
            select d.cidade
              from dbo_uni_pessoas.distritos d,
                   dbo_uni_pessoas.distritos_unificado du
             where du.cidade_correios   = d.cidade
               and du.distrito_correios = d.distrito
               and du.cidade_sane       = :cidadeNascimentoSane
               and du.distrito_sane     = 1
        """)
                    .setParameter("cidadeNascimentoSane", cidadeNascimentoSane)
                    .getSingleResult();

            return num(result);

        } catch (Exception e) {
            System.err.println("ERRO AO BUSCAR CIDADE NASCIMENTO SANE >>>>>>>>>>>>>>>>>>> " + e.getMessage());
            return null;
        }
    }

    public void inserirEnderecosUnicosSane(Long pessoaId, List<Object[]> origens) {
        if (origens == null || origens.isEmpty()) return;

        Set<String> chaves = new HashSet<>();
        boolean primeiro = true;

        for (Object[] sane : origens) {
            EnderecoResolvido end = buscarEnderecoPorMapeamentoSane(sane);

            if (end == null || end.bairroId() == null || end.logradouroId() == null) {
                continue;
            }

            Long numero = num(sane[29]);
            String chave = end.bairroId() + "|" + end.logradouroId() + "|" + (numero == null ? "" : numero);

            if (chaves.contains(chave)) {
                continue;
            }

            chaves.add(chave);

            manager.createNativeQuery("""
                insert into dbo_ccm_pessoas.enderecos
                (
                    id,
                    pessoa_id,
                    tipo_endereco,
                    cep_id,
                    bairro_id,
                    logradouro_id,
                    numero,
                    complemento,
                    principal
                )
                values
                (
                    seq_enderecos.nextval,
                    :pessoaId,
                    0,
                    :cepId,
                    :bairroId,
                    :logradouroId,
                    :numero,
                    :complemento,
                    :principal
                )
            """)
                    .setParameter("pessoaId", pessoaId)
                    .setParameter("cepId", end.cepId())
                    .setParameter("bairroId", end.bairroId())
                    .setParameter("logradouroId", end.logradouroId())
                    .setParameter("numero", numero)
                    .setParameter("complemento", str(sane[30]))
                    .setParameter("principal", primeiro ? "S" : "N")
                    .executeUpdate();

            primeiro = false;
        }
    }

    private EnderecoResolvido buscarEnderecoPorMapeamentoSane(Object[] sane) {
        Long cidade = num(sane[25]);
        Long distrito = num(sane[26]);
        Long bairro = num(sane[27]);
        Long logradouro = num(sane[28]);
        Long numero = num(sane[29]);
        Long cepInformado = num(sane[31]);

        if (cidade == null || distrito == null) return null;

        Long bairroId = buscarBairroCcmSane(cidade, distrito, bairro);
        Long logradouroId = buscarLogradouroCcmSane(cidade, distrito, logradouro);

        if (bairroId == null || logradouroId == null) return null;

        Long cepId = buscarCepIdCompativel(cepInformado, bairroId, logradouroId, numero);

        return new EnderecoResolvido(bairroId, logradouroId, cepId);
    }

    private Long buscarBairroCcmSane(Long cidade, Long distrito, Long bairro) {
        if (cidade == null || distrito == null || bairro == null) return null;

        try {
            Object result = manager.createNativeQuery("""
                select codigo_ccm
                  from (
                        select codigo_ccm
                          from (
                                select b.codigo_ccm,
                                       1 as prioridade
                                  from dbo_uni_pessoas.bairros_unificado bu
                                  join dbo_uni_pessoas.bairros b
                                    on b.cidade   = bu.cidade_correios
                                   and b.distrito = bu.distrito_correios
                                   and b.bairro   = bu.bairro_correios
                                 where bu.cidade_sane   = :cidade
                                   and bu.distrito_sane = :distrito
                                   and bu.bairro_sane   = :bairro
            
                                union all
            
                                select b.codigo_ccm,
                                       2 as prioridade
                                  from dbo_uni_pessoas.bairros_unificado_auxiliar bua
                                  join dbo_uni_pessoas.bairros_unificado bu
                                    on bu.id = bua.id_bairros_unificado
                                  join dbo_uni_pessoas.bairros b
                                    on b.cidade   = bu.cidade_correios
                                   and b.distrito = bu.distrito_correios
                                   and b.bairro   = bu.bairro_correios
                                 where bua.cidade_sane   = :cidade
                                   and bua.distrito_sane = :distrito
                                   and bua.bairro_sane   = :bairro
                               )
                         order by prioridade, codigo_ccm
                       )
                 where rownum = 1
            """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .setParameter("bairro", bairro)
                    .getSingleResult();

            return num(result);
        } catch (Exception e) {
            return null;
        }
    }

    private Long buscarLogradouroCcmSane(Long cidade, Long distrito, Long logradouro) {
        if (cidade == null || distrito == null || logradouro == null) return null;

        try {
            Object result = manager.createNativeQuery("""
                select codigo_ccm
                  from (
                        select l.codigo_ccm
                          from dbo_uni_pessoas.logradouros_unificado lu
                          join dbo_uni_pessoas.logradouros l
                            on l.cidade = lu.cidade_correios
                           and l.distrito = lu.distrito_correios
                           and l.logradouro = lu.logradouro_correios
                         where lu.cidade_sane = :cidade
                           and lu.distrito_sane = :distrito
                           and lu.logradouro_sane = :logradouro
                         order by l.codigo_ccm
                       )
                 where rownum = 1
            """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .setParameter("logradouro", logradouro)
                    .getSingleResult();

            return num(result);
        } catch (Exception e) {
            return null;
        }
    }
    private Long buscarCepIdCompativel(Long cepInformado, Long bairroId, Long logradouroId, Long numero) {

        Long cepId = buscarCepPorBairroLogradouroCepNumero(cepInformado, bairroId, logradouroId, numero);

        if (cepId != null) {
            return cepId;
        }

        return buscarCepPorLogradouroNumero(logradouroId, numero);
    }
    private Long buscarCepPorBairroLogradouroCepNumero(
            Long cepInformado,
            Long bairroId,
            Long logradouroId,
            Long numero
    ) {
        try {
            Object result = manager.createNativeQuery("""
            select id
              from (
                    select c.id
                      from dbo_ccm_pessoas.ceps c
                     where c.bairro_id = :bairroId
                       and c.logradouro_id = :logradouroId
                       and (:cepInformado is null or c.cep = :cepInformado)
                     order by
                       case
                         when :numero is not null
                          and c.numero_ini is not null
                          and c.numero_fim is not null
                          and :numero between c.numero_ini and c.numero_fim
                         then 0
                         else 1
                       end,
                       c.id
                   )
             where rownum = 1
        """)
                    .setParameter("bairroId", bairroId)
                    .setParameter("logradouroId", logradouroId)
                    .setParameter("cepInformado", cepInformado)
                    .setParameter("numero", numero)
                    .getSingleResult();

            return num(result);

        } catch (Exception e) {
            return null;
        }
    }
    private Long buscarCepPorLogradouroNumero(Long logradouroId, Long numero) {
        if (logradouroId == null || numero == null) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
            select id
              from (
                    select c.id
                      from dbo_ccm_pessoas.ceps c
                     where c.logradouro_id = :logradouroId
                       and c.numero_ini is not null
                       and c.numero_fim is not null
                       and :numero between c.numero_ini and c.numero_fim
                     order by c.id
                   )
             where rownum = 1
        """)
                    .setParameter("logradouroId", logradouroId)
                    .setParameter("numero", numero)
                    .getSingleResult();

            return num(result);

        } catch (Exception e) {
            return null;
        }
    }
    public Long num(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();

        String s = String.valueOf(o).trim();
        if (s.isEmpty()) return null;

        return Long.valueOf(s);
    }

    public Integer integer(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();

        String s = String.valueOf(o).trim();
        if (s.isEmpty()) return null;

        return Integer.valueOf(s);
    }

    public String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    public String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    public String normalizarContato(Long tipoContato, Object contato) {
        if (contato == null) return null;

        String valor = String.valueOf(contato).trim();
        if (valor.isBlank()) return null;

        if (tipoContato.equals(0L)
                || tipoContato.equals(1L)
                || tipoContato.equals(5L)
                || tipoContato.equals(6L)) {

            String digitos = valor.replaceAll("\\D", "");
            return digitos.isBlank() ? null : digitos;
        }

        if (tipoContato.equals(3L) || tipoContato.equals(4L)) {
            return valor.toLowerCase().trim();
        }

        return valor;
    }

    public record EnderecoResolvido(
            Long bairroId,
            Long logradouroId,
            Long cepId
    ) {}

    public Long countContato(
            Long pessoaId,
            Long tipoContato,
            String contato
    ) {

        return ((Number) manager.createNativeQuery("""
        select count(1)
          from dbo_ccm_pessoas.contatos c
         where c.pessoa_id = :pessoaId
           and c.tipo_contato = :tipoContato
           and upper(trim(c.contato)) = upper(trim(:contato))
    """)
                .setParameter("pessoaId", pessoaId)
                .setParameter("tipoContato", tipoContato)
                .setParameter("contato", contato)
                .getSingleResult()).longValue();
    }

    /*-------------------------------------------*/

    public void complementarEnderecosSane(Long pessoaId, List<Object[]> origens) {
        if (origens == null || origens.isEmpty()) {
            return;
        }

        Set<String> chavesProcessadas = new HashSet<>();

        for (Object[] sane : origens) {
            EnderecoResolvido end = buscarEnderecoPorMapeamentoSane(sane);

            if (end == null || end.bairroId() == null || end.logradouroId() == null) {
                continue;
            }

            Long numero = num(sane[29]);

            String chave = end.bairroId() + "|"
                    + end.logradouroId() + "|"
                    + (numero == null ? "" : numero);

            if (chavesProcessadas.contains(chave)) {
                continue;
            }

            chavesProcessadas.add(chave);

            Long existe = ((Number) manager.createNativeQuery("""
            select count(1)
              from dbo_ccm_pessoas.enderecos e
             where e.pessoa_id = :pessoaId
               and e.bairro_id = :bairroId
               and e.logradouro_id = :logradouroId
               and nvl(e.numero, 0) = nvl(:numero, 0)
        """)
                    .setParameter("pessoaId", pessoaId)
                    .setParameter("bairroId", end.bairroId())
                    .setParameter("logradouroId", end.logradouroId())
                    .setParameter("numero", numero)
                    .getSingleResult()).longValue();

            if (existe > 0) {
                continue;
            }

            manager.createNativeQuery("""
            insert into dbo_ccm_pessoas.enderecos
            (
                id,
                pessoa_id,
                tipo_endereco,
                cep_id,
                bairro_id,
                logradouro_id,
                numero,
                complemento,
                principal
            )
            values
            (
                seq_enderecos.nextval,
                :pessoaId,
                0,
                :cepId,
                :bairroId,
                :logradouroId,
                :numero,
                :complemento,
                'N'
            )
        """)
                    .setParameter("pessoaId", pessoaId)
                    .setParameter("cepId", end.cepId())
                    .setParameter("bairroId", end.bairroId())
                    .setParameter("logradouroId", end.logradouroId())
                    .setParameter("numero", numero)
                    .setParameter("complemento", str(sane[30]))
                    .executeUpdate();
        }
    }

}