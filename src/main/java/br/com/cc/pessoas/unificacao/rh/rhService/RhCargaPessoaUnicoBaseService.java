package br.com.cc.pessoas.unificacao.rh.rhService;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhPessoa;
import br.com.cc.pessoas.unificacao.rh.rhRepository.RhPessoaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

public abstract class RhCargaPessoaUnicoBaseService {

    @Autowired
    protected RhPessoaRepository rhPessoaRepository;

    @PersistenceContext
    protected EntityManager manager;

    protected abstract String getFisicaJuridica();

    protected abstract Long getTipoEndereco();

    protected abstract void processarPessoa(RhPessoa pessoa);

    @Transactional
    public void processarPessoaUnica(Long pessoaId) {
        RhPessoa pessoa = rhPessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new RuntimeException("Pessoa RH não encontrada: " + pessoaId));

        if (!getFisicaJuridica().equalsIgnoreCase(pessoa.getFisicaJuridica())) {
            throw new RuntimeException("Pessoa RH não é do tipo esperado: " + pessoaId);
        }

        processarPessoa(pessoa);
    }

    protected void inserirPessoaRh(Long idPessoa, RhPessoa pessoa, Long tipoPessoa) {
        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.PESSOAS
            (
                ID,
                TIPO_PESSOA_ID,
                NOME,
                DATA_CADASTRO,
                FISICA_JURIDICA,
                SITUACAO_ID
            )
            values
            (
                :id,
                :tipoPessoaId,
                :nome,
                :dataCadastro,
                :fisicaJuridica,
                :situacaoId
            )
        """)
                .setParameter("id", idPessoa)
                .setParameter("tipoPessoaId", tipoPessoa)
                .setParameter("nome", upper(pessoa.getNome()))
                .setParameter("dataCadastro", pessoa.getDataCadastro())
                .setParameter("fisicaJuridica", pessoa.getFisicaJuridica())
                .setParameter("situacaoId", 1L)
                .executeUpdate();
    }

    protected void inserirEnderecoPrincipalRh(Long idPessoa, RhPessoa pessoa) {

        // Endereço fixo.
        EnderecoCarga endereco = resolverEnderecoFixoRh(pessoa);

        if (endereco == null) {
            endereco = buscarEnderecoPorMapeamentoRh(pessoa);
        }

        if (endereco == null || endereco.bairroId == null || endereco.logradouroId == null) {
            endereco = buscarEnderecoPorCepRh(pessoa, pessoa.getNumero());
        }

        if (endereco == null || endereco.bairroId == null || endereco.logradouroId == null) {
            return;
        }

        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.ENDERECOS
            (
                ID,
                PESSOA_ID,
                TIPO_ENDERECO,
                BAIRRO_ID,
                LOGRADOURO_ID,
                NUMERO,
                COMPLEMENTO,
                CEP_ID,
                BANCO,
                PRINCIPAL
            )
            values
            (
                SEQ_ENDERECOS.nextval,
                :pessoaId,
                :tipoEndereco,
                :bairroId,
                :logradouroId,
                :numero,
                :complemento,
                :cepId,
                :banco,
                :principal
            )
        """)
                .setParameter("pessoaId", idPessoa)
                .setParameter("tipoEndereco", getTipoEndereco())
                .setParameter("bairroId", endereco.bairroId)
                .setParameter("logradouroId", endereco.logradouroId)
                .setParameter("numero", pessoa.getNumero())
                .setParameter("complemento", pessoa.getComplemento())
                .setParameter("cepId", endereco.cepId)
                .setParameter("banco", "RH")
                .setParameter("principal", "S")
                .executeUpdate();
    }

    protected EnderecoCarga resolverEnderecoFixoRh(RhPessoa pessoa) {
        if (pessoa == null) {
            return null;
        }

        Long cidade = pessoa.getCidade();
        Long bairro = pessoa.getBairro();
        Long distrito = pessoa.getDistrito();
        Long logradouro = pessoa.getLogradouro();

        boolean enderecoEspecial =
                (Long.valueOf(1L).equals(cidade) || Long.valueOf(9999L).equals(cidade)) &&
                        (
                                Long.valueOf(8888L).equals(bairro) ||
                                        Long.valueOf(9999L).equals(bairro) ||
                                        Long.valueOf(8888L).equals(logradouro) ||
                                        Long.valueOf(9999L).equals(logradouro) ||
                                        Long.valueOf(8888L).equals(distrito) ||
                                        Long.valueOf(9999L).equals(distrito)
                        );

        if (!enderecoEspecial) {
            return null;
        }

        Long distritoId = buscarCodigoCcmDistritoRh(cidade, distrito);

        if (distritoId == null) {
            return null;
        }

        /*
            Vou colocar isso aqui fixo se fizer carga novamente tem quer quais serão os codigos novos;
         */

        EnderecoCarga end = new EnderecoCarga();
        //end.logradouroId = buscarLogradouroPorNome(distritoId, pessoa.getLogradouroNome());
        end.logradouroId = 1105867L;
        //end.bairroId = buscarBairroPorNome(distritoId, pessoa.getBairroNome());
        end.bairroId = 83186L;
        end.cepId = 1444263L;

        return end;
    }

    protected EnderecoCarga buscarEnderecoPorMapeamentoRh(RhPessoa pessoa) {
        if (pessoa == null) {
            return null;
        }

        Long cidade = pessoa.getCidade();
        Long distrito = pessoa.getDistrito();
        Long bairro = pessoa.getBairro();
        Long logradouro = pessoa.getLogradouro();

        if (cidade == null || distrito == null) {
            return null;
        }

        EnderecoCarga end = new EnderecoCarga();

        try {
            Object resultLogradouro = manager.createNativeQuery("""
                select codigo_ccm
                   from (
                         select l.codigo_ccm
                           from dbo_uni_pessoas.logradouros_unificado lu
                           join dbo_uni_pessoas.logradouros l
                             on l.cidade = lu.cidade_correios
                            and l.distrito = lu.distrito_correios
                            and l.logradouro = lu.logradouro_correios
                          where lu.cidade_rh = :cidade
                            and lu.distrito_rh = :distrito
                            and lu.logradouro_rh = :logradouro
                          order by l.codigo_ccm
                        )
                  where rownum = 1
            """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .setParameter("logradouro", logradouro)
                    .getSingleResult();

            end.logradouroId = resultLogradouro == null ? null : ((Number) resultLogradouro).longValue();

        } catch (Exception e) {
            end.logradouroId = null;
        }

        try {
            Object resultBairro = manager.createNativeQuery("""
                select codigo_ccm
                      from (
                            select b.codigo_ccm
                              from dbo_uni_pessoas.bairros_unificado bu
                              join dbo_uni_pessoas.bairros b
                                on b.cidade = bu.cidade_correios
                               and b.distrito = bu.distrito_correios
                               and b.bairro = bu.bairro_correios
                             where bu.cidade_rh = :cidade
                               and bu.distrito_rh = :distrito
                               and bu.bairro_rh = :bairro
                             order by b.codigo_ccm
                           )
                     where rownum = 1
                """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .setParameter("bairro", bairro)
                    .getSingleResult();

            end.bairroId = resultBairro == null ? null : ((Number) resultBairro).longValue();

        } catch (Exception e) {
            end.bairroId = null;
        }

        end.cepId = null;

        if (end.logradouroId == null && end.bairroId == null) {
            return null;
        }

        if (end.logradouroId != null && end.bairroId != null) {
            buscarCepPorBairroLogradouroNumeroRh(pessoa, end);

            if (end.cepId == null || end.cepId == 0L) {
                buscarCepPorTabelaRh(cidade, distrito, logradouro, end);
            }
        }

        return end;
    }

    protected EnderecoCarga buscarEnderecoPorCepRh(RhPessoa pessoa, Long numero) {
        if (pessoa == null || pessoa.getCep() == null || cepInvalido(pessoa.getCep())) {
            return null;
        }

        String cepLimpo = String.valueOf(pessoa.getCep()).trim();

        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = manager.createNativeQuery("""
                select id, bairro_id, logradouro_id
                  from (
                        select c.id,
                               c.bairro_id,
                               c.logradouro_id,
                               case
                                   when :numero is not null
                                    and c.numero_ini is not null
                                    and c.numero_fim is not null
                                    and :numero between c.numero_ini and c.numero_fim
                                   then 0
                                   else 1
                               end as ordem
                          from dbo_ccm_pessoas.ceps c
                         where trim(c.cep) = :cep
                         order by ordem, c.id
                       )
                 where rownum = 1
            """)
                    .setParameter("cep", cepLimpo)
                    .setParameter("numero", numero)
                    .getResultList();

            if (rows == null || rows.isEmpty()) {
                return null;
            }

            Object[] row = rows.get(0);

            EnderecoCarga end = new EnderecoCarga();
            end.cepId = row[0] != null ? ((Number) row[0]).longValue() : null;
            end.bairroId = row[1] != null ? ((Number) row[1]).longValue() : null;
            end.logradouroId = row[2] != null ? ((Number) row[2]).longValue() : null;

            return end;

        } catch (Exception e) {
            return null;
        }
    }

    protected void buscarCepPorBairroLogradouroNumeroRh(RhPessoa pessoa, EnderecoCarga end) {
        try {
            Long numero = pessoa.getNumero();

            if (numero == null || numero == 0L) {
                numero = 1L;
            }

            Object resultCep = manager.createNativeQuery("""
                select id
                  from (
                        select c.id,
                               case
                                   when :numero is not null
                                    and c.numero_ini is not null
                                    and c.numero_fim is not null
                                    and :numero between c.numero_ini and c.numero_fim
                                   then 0
                                   else 1
                               end as ordem
                          from dbo_ccm_pessoas.ceps c
                         where c.bairro_id = :bairroId
                           and c.logradouro_id = :logradouroId
                         order by ordem, c.id
                       )
                 where rownum = 1
            """)
                    .setParameter("bairroId", end.bairroId)
                    .setParameter("logradouroId", end.logradouroId)
                    .setParameter("numero", numero)
                    .getSingleResult();

            end.cepId = resultCep == null ? null : ((Number) resultCep).longValue();

        } catch (Exception e) {
            end.cepId = null;
        }
    }

    protected void buscarCepPorTabelaRh(Long cidade, Long distrito, Long logradouro, EnderecoCarga end) {
        try {
            Object resultCepRh = manager.createNativeQuery("""
                select cep
                  from (
                        select cep
                          from dbo_rh.ceps
                         where cidade = :cidade
                           and distrito = :distrito
                           and logradouro = :logradouro
                         order by cep
                       )
                 where rownum = 1
            """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .setParameter("logradouro", logradouro)
                    .getSingleResult();

            Long cepRh = resultCepRh == null ? null : ((Number) resultCepRh).longValue();

            if (cepRh == null) {
                end.cepId = null;
                return;
            }

            Object resultCepId = manager.createNativeQuery("""
                select id
                  from (
                        select id
                          from dbo_ccm_pessoas.ceps
                         where cep = :cep
                         order by id
                       )
                 where rownum = 1
            """)
                    .setParameter("cep", cepRh)
                    .getSingleResult();

            end.cepId = resultCepId == null ? null : ((Number) resultCepId).longValue();

        } catch (Exception e) {
            end.cepId = null;
        }
    }

    protected void inserirContatosRh(Long idPessoa, RhPessoa pessoa) {
        if (pessoa.getTelefone() != null && pessoa.getTelefone() != 0L) {
            inserirContato(idPessoa, 0L, montarTelefone(pessoa.getDddTelefone(), pessoa.getTelefone()));
        }

        if (pessoa.getCelular() != null && pessoa.getCelular() != 0L) {
            inserirContato(idPessoa, 1L, montarTelefone(pessoa.getDddCelular(), pessoa.getCelular()));
        }

        if (pessoa.getEmail() != null && !pessoa.getEmail().isBlank()) {
            inserirContato(idPessoa, 3L, pessoa.getEmail());
        }

        if (pessoa.getPaginaWeb() != null && !pessoa.getPaginaWeb().isBlank()) {
            inserirContato(idPessoa, 4L, pessoa.getPaginaWeb());
        }

        if (pessoa.getRecado() != null && pessoa.getRecado() != 0L) {
            inserirContato(idPessoa, 5L, montarTelefone(pessoa.getDddRecado(), pessoa.getRecado()));
        }

        if (pessoa.getFax() != null && pessoa.getFax() != 0L) {
            inserirContato(idPessoa, 6L, pessoa.getFax());
        }

        if (pessoa.getWhatsapp() != null && pessoa.getWhatsapp() != 0L) {
            inserirContato(idPessoa, 1L, pessoa.getWhatsapp());
        }

        if (pessoa.getInstagram() != null && !pessoa.getInstagram().isBlank()) {
            inserirContato(idPessoa, 4L, pessoa.getInstagram());
        }

        if (pessoa.getFacebook() != null && !pessoa.getFacebook().isBlank()) {
            inserirContato(idPessoa, 4L, pessoa.getFacebook());
        }
    }

    protected void inserirContato(Long idPessoa, Long tipoContato, Object contato) {
        if (contato == null) {
            return;
        }

        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.CONTATOS
            (
                ID,
                PESSOA_ID,
                TIPO_CONTATO,
                CONTATO
            )
            values
            (
                SEQ_CONTATOS.nextval,
                :pessoaId,
                :tipoContato,
                :contato
            )
        """)
                .setParameter("pessoaId", idPessoa)
                .setParameter("tipoContato", tipoContato)
                .setParameter("contato", contato)
                .executeUpdate();
    }

    protected Long buscarCodigoCcmDistritoRh(Long cidade, Long distrito) {
        if (cidade == null || distrito == null) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
                select d.codigo_ccm
                  from dbo_uni_pessoas.distritos d,
                       dbo_uni_pessoas.distritos_unificado du
                 where d.cidade = du.cidade_correios
                   and d.distrito = du.distrito_correios
                   and du.cidade_rh = :cidade
                   and du.distrito_rh = :distrito
            """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .getSingleResult();

            return result == null ? null : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    protected Long buscarCidadeNascimentoCcmRh(Long cidadeNascimento) {
        if (cidadeNascimento == null || cidadeNascimento == 0L) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
                select d.cidade
                  from dbo_uni_pessoas.distritos d,
                       dbo_uni_pessoas.distritos_unificado du
                 where du.cidade_correios = d.cidade
                   and du.distrito_correios = d.distrito
                   and du.cidade_rh = :cidadeNascimento
                   and du.distrito_rh = 1
            """)
                    .setParameter("cidadeNascimento", cidadeNascimento)
                    .getSingleResult();

            return result == null ? null : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    protected Long buscarLogradouroPorNome(Long distritoId, String logradouroNome) {
        if (distritoId == null || logradouroNome == null || logradouroNome.isBlank()) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
                select id
                  from dbo_ccm_pessoas.logradouros
                 where distrito_id = :distritoId
                   and upper(nome) = upper(:logradouroNome)
            """)
                    .setParameter("distritoId", distritoId)
                    .setParameter("logradouroNome", logradouroNome.trim())
                    .getSingleResult();

            return result == null ? null : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    protected Long buscarBairroPorNome(Long distritoId, String bairroNome) {
        if (distritoId == null || bairroNome == null || bairroNome.isBlank()) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
                select id
                  from dbo_ccm_pessoas.bairros
                 where distrito_id = :distritoId
                   and upper(nome) = upper(:bairroNome)
            """)
                    .setParameter("distritoId", distritoId)
                    .setParameter("bairroNome", bairroNome.trim())
                    .getSingleResult();

            return result == null ? null : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    protected Long getTipoPessoaRh(RhPessoa pessoa) {
        return pessoa.getRhTipoPessoa() != null
                ? Long.valueOf(pessoa.getRhTipoPessoa().getTipoPessoa())
                : 1L;
    }

    protected Long getNextVal(String sequence) {
        return ((Number) manager
                .createNativeQuery("select " + sequence + ".nextval from dual")
                .getSingleResult()).longValue();
    }

    protected String upper(String valor) {
        return valor == null ? null : valor.toUpperCase().trim();
    }

    protected Object montarTelefone(Long ddd, Long numero) {
        if (numero == null) {
            return null;
        }

        if (ddd == null || ddd == 0L) {
            return numero;
        }

        return Long.valueOf(String.valueOf(ddd) + numero);
    }

    protected boolean cepInvalido(Long cep) {
        if (cep == null) {
            return true;
        }

        return cep < 10000000L || cep > 99999999L || CEPS_INVALIDOS_FIXOS.contains(cep);
    }

    protected static final Set<Long> CEPS_INVALIDOS_FIXOS = Set.of(
            38100000L
    );

    protected static class EnderecoCarga {
        protected Long bairroId;
        protected Long logradouroId;
        protected Long cepId;
    }
}