package br.com.cc.pessoas.unificacao.rh.rhService;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhPessoa;
import br.com.cc.pessoas.unificacao.rh.rhRepository.RhPessoaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RhCargaPessoaJaExisteCadUnicoService {

    private final RhPessoaRepository rhPessoaRepository;

    @PersistenceContext
    private EntityManager manager;

    @Transactional
    public void processarPessoaUnica(Long pessoaId) {
        RhPessoa pessoa = rhPessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new RuntimeException("Pessoa RH não encontrada: " + pessoaId));

        Long pessoaCcmId = buscarPessoaExistenteCadUnico(pessoa);

        if (pessoaCcmId == null) {
            throw new RuntimeException("Pessoa RH não localizada no Cadastro Único existente: " + pessoaId);
        }

        if (jaExisteVinculoRh(pessoa)) {
            return;
        }

        inserirVinculoRhCadUnico(pessoaCcmId, pessoa);

        complementarPessoa(pessoaCcmId, pessoa);
        complementarDadosPf(pessoaCcmId, pessoa);
        inserirDocumentosSeNaoExistirem(pessoaCcmId, pessoa);
        inserirContatosSeNaoExistirem(pessoaCcmId, pessoa);
        inserirEnderecoSeNaoExistir(pessoaCcmId, pessoa);


    }

    private Long buscarPessoaExistenteCadUnico(RhPessoa pessoa) {
        try {
            Object result = manager.createNativeQuery("""
                select pessoas_cd_unico
                  from (
                        select cup.pessoas_cd_unico
                          from dbo_ccm_pessoas.cad_unico_pessoa cup
                         where cup.cpf_cnpj = :cpfCnpj
                           and cup.banco = 'PESSOAS'
                           and replace(transf_caracte(cup.nome), ' ', '') =
                               replace(transf_caracte(:nome), ' ', '')
                           and cup.pessoas_cd_unico is not null
                         order by cup.id
                       )
                 where rownum = 1
            """)
                    .setParameter("cpfCnpj", pessoa.getCgcCpf())
                    .setParameter("nome", pessoa.getNome())
                    .getSingleResult();

            return result == null ? null : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    private boolean jaExisteVinculoRh(RhPessoa pessoa) {
        Object result = manager.createNativeQuery("""
            select count(*)
              from dbo_ccm_pessoas.cad_unico_pessoa cup
             where cup.cd_origem = :cdOrigem
               and cup.banco = 'RH'
        """)
                .setParameter("cdOrigem", pessoa.getPessoa())
                .getSingleResult();

        return result != null && ((Number) result).longValue() > 0;
    }

    private void inserirVinculoRhCadUnico(Long pessoaCcmId, RhPessoa pessoa) {
        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.CAD_UNICO_PESSOA
            (
                 ID,
                 CD_ORIGEM,
                 TIPO_PESSOA,
                 NOME,
                 FISICA_JURIDICA,
                 DATA_CADASTRO,
                 CPF_CNPJ,
                 DATA_NASCIMENTO,
                 ESTADO_CIVIL,
                 SEXO,
                 CIDADE_NASCIMENTO,
                 EMAIL,
                 BANCO,
                 PESSOAS_CD_UNICO
            )
            values
            (
                DBO_CCM_PESSOAS.SEQ_CAD_UNICO_PESSOA.nextval,
                :cdOrigem,
                :tipoPessoa,
                :nome,
                :fisicaJuridica,
                :dataCadastro,
                :cpfCnpj,
                :dataNascimento,
                :estadoCivil,
                :sexo,
                :cidadeNascimento,
                :email,
                :banco,
                :pessoasCdUnico
            )
        """)
                .setParameter("cdOrigem", pessoa.getPessoa())
                .setParameter("tipoPessoa", getTipoPessoaRh(pessoa))
                .setParameter("nome", upper(pessoa.getNome()))
                .setParameter("fisicaJuridica", pessoa.getFisicaJuridica())
                .setParameter("dataCadastro", pessoa.getDataCadastro())
                .setParameter("cpfCnpj", pessoa.getCgcCpf())
                .setParameter("dataNascimento", pessoa.getDataNascimento())
                .setParameter("estadoCivil", pessoa.getEstadoCivil())
                .setParameter("sexo", pessoa.getSexo())
                .setParameter("cidadeNascimento", pessoa.getCidadeNascimento())
                .setParameter("email", pessoa.getEmail())
                .setParameter("banco", "RH")
                .setParameter("pessoasCdUnico", pessoaCcmId)
                .executeUpdate();
    }

    private Long getTipoPessoaRh(RhPessoa pessoa) {
        return pessoa.getRhTipoPessoa() != null
                ? Long.valueOf(pessoa.getRhTipoPessoa().getTipoPessoa())
                : 1L;
    }

    private String upper(String valor) {
        return valor == null ? null : valor.toUpperCase().trim();
    }

    private void complementarPessoa(Long pessoaCcmId, RhPessoa pessoa) {
        manager.createNativeQuery("""
        update DBO_CCM_PESSOAS.PESSOAS p
           set p.observacao = case
                                when (p.observacao is null or trim(p.observacao) = '')
                                 and :observacao is not null
                                then :observacao
                                else p.observacao
                              end
         where p.id = :id
    """)
                .setParameter("id", pessoaCcmId)
                .setParameter("observacao", pessoa.getVip())
                .executeUpdate();
    }

    private void complementarDadosPf(Long pessoaCcmId, RhPessoa pessoa) {
        String estadoCivil = mapearEstadoCivil(pessoa.getEstadoCivil());
        Long cidadeNascimentoCcm = buscarCidadeNascimentoCcmRh(pessoa.getCidadeNascimento());

        atualizarTextoSeVazio(pessoaCcmId, "NOME_SOCIAL", pessoa.getNomeSocial());
        atualizarTextoSeVazio(pessoaCcmId, "SEXO", pessoa.getSexo());
        atualizarTextoSeVazio(pessoaCcmId, "ESTADO_CIVIL", estadoCivil);
        atualizarTextoSeVazio(pessoaCcmId, "MAE", pessoa.getMae());
        atualizarTextoSeVazio(pessoaCcmId, "PAI", pessoa.getPai());

        if (cidadeNascimentoCcm != null) {
            manager.createNativeQuery("""
            update DBO_CCM_PESSOAS.DADOS_PF
               set LOCAL_NASCIMENTO_ID = :valor
             where ID = :id
               and LOCAL_NASCIMENTO_ID is null
        """)
                    .setParameter("valor", cidadeNascimentoCcm)
                    .setParameter("id", pessoaCcmId)
                    .executeUpdate();
        }

        if (pessoa.getDataNascimento() != null && !dataFake1900(pessoa.getDataNascimento())) {
            manager.createNativeQuery("""
            update DBO_CCM_PESSOAS.DADOS_PF
               set DATA_NASCIMENTO = :dataNascimento
             where ID = :id
               and (
                    DATA_NASCIMENTO is null
                    or trunc(DATA_NASCIMENTO) = to_date('01/01/1900', 'DD/MM/YYYY')
               )
        """)
                    .setParameter("dataNascimento", pessoa.getDataNascimento())
                    .setParameter("id", pessoaCcmId)
                    .executeUpdate();
        }
    }

    private void atualizarTextoSeVazio(Long pessoaCcmId, String coluna, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }

        manager.createNativeQuery("""
        update DBO_CCM_PESSOAS.DADOS_PF
           set %s = :valor
         where ID = :id
           and (%s is null or trim(%s) = '')
    """.formatted(coluna, coluna, coluna))
                .setParameter("valor", valor.trim())
                .setParameter("id", pessoaCcmId)
                .executeUpdate();
    }

    private boolean dataFake1900(LocalDateTime data) {
        return data != null
                && data.getYear() == 1900
                && data.getMonthValue() == 1
                && data.getDayOfMonth() == 1;
    }

    private Long buscarCidadeNascimentoCcmRh(Long cidadeNascimento) {
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

    private String mapearEstadoCivil(String estadoCivil) {
        if (estadoCivil == null || estadoCivil.isBlank()) {
            return null;
        }

        return switch (estadoCivil.trim()) {
            case "1" -> "3";
            case "2" -> "1";
            case "3" -> "7";
            case "4" -> "2";
            case "5" -> "5";
            case "6" -> "6";
            case "7" -> "8";
            case "8" -> "4";
            default -> null;
        };
    }

    private void inserirDocumentosSeNaoExistirem(Long pessoaCcmId, RhPessoa pessoa) {

        // Documento principal
        if (pessoa.getNumeroDocto() != null && !pessoa.getNumeroDocto().isBlank()) {

            Long tipoDocumento = pessoa.getRhTipoDocumento() != null
                    ? Long.valueOf(pessoa.getRhTipoDocumento().getTipoDocumento())
                    : 5L;

            if (tipoDocumento == 2L) {
                tipoDocumento = 0L;
            }

            if (tipoDocumento == 15L) {
                tipoDocumento = 5L;
            }

            String numeroDocumento = pessoa.getNumeroDocto().trim();

            if (!existeDocumento(pessoaCcmId, tipoDocumento, numeroDocumento)) {
                manager.createNativeQuery("""
                insert into DBO_CCM_PESSOAS.DOCUMENTOS
                (
                    ID,
                    PESSOA_ID,
                    TIPO_DOCUMENTO,
                    NUMERO_DOCUMENTO,
                    ORGAO_EXPEDIDOR,
                    DATA_EXPEDICAO
                )
                values
                (
                    SEQ_DOCUMENTOS.nextval,
                    :pessoaId,
                    :tipoDocumento,
                    :numeroDocumento,
                    :orgaoExpedidor,
                    :dataExpedicao
                )
            """)
                        .setParameter("pessoaId", pessoaCcmId)
                        .setParameter("tipoDocumento", tipoDocumento)
                        .setParameter("numeroDocumento", numeroDocumento)
                        .setParameter("orgaoExpedidor", pessoa.getOrgaoDocto())
                        .setParameter("dataExpedicao", pessoa.getEmissaoDocto())
                        .executeUpdate();
            }
        }

        // Título eleitoral
        if (pessoa.getTituloEleitoral() != null && pessoa.getTituloEleitoral() != 0L) {

            Long tipoTitulo = 6L;
            String numeroTitulo = String.valueOf(pessoa.getTituloEleitoral());

            if (!existeDocumento(pessoaCcmId, tipoTitulo, numeroTitulo)) {
                manager.createNativeQuery("""
                insert into DBO_CCM_PESSOAS.DOCUMENTOS
                (
                    ID,
                    PESSOA_ID,
                    TIPO_DOCUMENTO,
                    NUMERO_DOCUMENTO,
                    ZONA,
                    SECAO
                )
                values
                (
                    SEQ_DOCUMENTOS.nextval,
                    :pessoaId,
                    :tipoDocumento,
                    :numeroDocumento,
                    :zona,
                    :secao
                )
            """)
                        .setParameter("pessoaId", pessoaCcmId)
                        .setParameter("tipoDocumento", tipoTitulo)
                        .setParameter("numeroDocumento", numeroTitulo)
                        .setParameter("zona", pessoa.getZona())
                        .setParameter("secao", pessoa.getSecao())
                        .executeUpdate();
            }
        }
    }

    private boolean existeDocumento(Long pessoaCcmId, Long tipoDocumento, String numeroDocumento) {
        if (pessoaCcmId == null || tipoDocumento == null || numeroDocumento == null || numeroDocumento.isBlank()) {
            return true;
        }

        Object result = manager.createNativeQuery("""
        select count(*)
          from DBO_CCM_PESSOAS.DOCUMENTOS d
         where d.pessoa_id = :pessoaId
           and d.tipo_documento = :tipoDocumento
           and trim(upper(d.numero_documento)) = trim(upper(:numeroDocumento))
    """)
                .setParameter("pessoaId", pessoaCcmId)
                .setParameter("tipoDocumento", tipoDocumento)
                .setParameter("numeroDocumento", numeroDocumento)
                .getSingleResult();

        return result != null && ((Number) result).longValue() > 0;
    }

    private void inserirContatosSeNaoExistirem(Long pessoaCcmId, RhPessoa pessoa) {
        inserirContatoSeNaoExistir(pessoaCcmId, 0L, montarTelefone(pessoa.getDddTelefone(), pessoa.getTelefone()));
        inserirContatoSeNaoExistir(pessoaCcmId, 1L, montarTelefone(pessoa.getDddCelular(), pessoa.getCelular()));
        inserirContatoSeNaoExistir(pessoaCcmId, 1L, pessoa.getWhatsapp());
        inserirContatoSeNaoExistir(pessoaCcmId, 3L, pessoa.getEmail());
        inserirContatoSeNaoExistir(pessoaCcmId, 4L, pessoa.getPaginaWeb());
        inserirContatoSeNaoExistir(pessoaCcmId, 4L, pessoa.getInstagram());
        inserirContatoSeNaoExistir(pessoaCcmId, 4L, pessoa.getFacebook());
        inserirContatoSeNaoExistir(pessoaCcmId, 5L, montarTelefone(pessoa.getDddRecado(), pessoa.getRecado()));
        inserirContatoSeNaoExistir(pessoaCcmId, 6L, pessoa.getFax());
    }

    private void inserirContatoSeNaoExistir(Long pessoaCcmId, Long tipoContato, Object contato) {
        String contatoNormalizado = normalizarContato(tipoContato, contato);

        if (contatoNormalizado == null || contatoNormalizado.isBlank()) {
            return;
        }

        if (existeContato(pessoaCcmId, tipoContato, contatoNormalizado)) {
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
                .setParameter("pessoaId", pessoaCcmId)
                .setParameter("tipoContato", tipoContato)
                .setParameter("contato", contatoNormalizado)
                .executeUpdate();
    }

    private boolean existeContato(Long pessoaCcmId, Long tipoContato, String contatoNormalizado) {
        Object result = manager.createNativeQuery("""
        select count(*)
          from DBO_CCM_PESSOAS.CONTATOS c
         where c.pessoa_id = :pessoaId
           and c.tipo_contato = :tipoContato
           and trim(lower(c.contato)) = trim(lower(:contato))
    """)
                .setParameter("pessoaId", pessoaCcmId)
                .setParameter("tipoContato", tipoContato)
                .setParameter("contato", contatoNormalizado)
                .getSingleResult();

        return result != null && ((Number) result).longValue() > 0;
    }

    private String normalizarContato(Long tipoContato, Object contato) {
        if (contato == null) {
            return null;
        }

        String valor = String.valueOf(contato).trim();

        if (valor.isBlank() || "0".equals(valor)) {
            return null;
        }

        if (tipoContato.equals(0L) || tipoContato.equals(1L) || tipoContato.equals(5L) || tipoContato.equals(6L)) {
            String digitos = valor.replaceAll("\\D", "");
            return digitos.isBlank() || "0".equals(digitos) ? null : digitos;
        }

        if (tipoContato.equals(3L) || tipoContato.equals(4L)) {
            return valor.toLowerCase();
        }

        return valor;
    }

    private Object montarTelefone(Long ddd, Long numero) {
        if (numero == null || numero == 0L) {
            return null;
        }

        if (ddd == null || ddd == 0L) {
            return numero;
        }

        return Long.valueOf(String.valueOf(ddd) + numero);
    }

    private void inserirEnderecoSeNaoExistir(Long pessoaCcmId, RhPessoa pessoa) {
        EnderecoCarga endereco = buscarEnderecoPorMapeamentoRh(pessoa);

        if (endereco == null || endereco.bairroId == null || endereco.logradouroId == null) {
            endereco = buscarEnderecoPorCepRh(pessoa, pessoa.getNumero());
        }

        if (endereco == null || endereco.bairroId == null || endereco.logradouroId == null) {
            return;
        }

        if (existeEndereco(pessoaCcmId, endereco, pessoa.getNumero())) {
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
                .setParameter("pessoaId", pessoaCcmId)
                .setParameter("tipoEndereco", 0L)
                .setParameter("bairroId", endereco.bairroId)
                .setParameter("logradouroId", endereco.logradouroId)
                .setParameter("numero", pessoa.getNumero())
                .setParameter("complemento", pessoa.getComplemento())
                .setParameter("cepId", endereco.cepId)
                .setParameter("banco", "RH")
                .setParameter("principal", "N")
                .executeUpdate();
    }

    private boolean existeEndereco(Long pessoaCcmId, EnderecoCarga endereco, Long numero) {
        Long numeroBusca = numero == null ? 0L : numero;

        Object result = manager.createNativeQuery("""
        select count(*)
          from DBO_CCM_PESSOAS.ENDERECOS e
         where e.pessoa_id = :pessoaId
           and e.bairro_id = :bairroId
           and e.logradouro_id = :logradouroId
           and nvl(e.numero, 0) = :numero
    """)
                .setParameter("pessoaId", pessoaCcmId)
                .setParameter("bairroId", endereco.bairroId)
                .setParameter("logradouroId", endereco.logradouroId)
                .setParameter("numero", numeroBusca)
                .getSingleResult();

        return result != null && ((Number) result).longValue() > 0;
    }

    private EnderecoCarga buscarEnderecoPorMapeamentoRh(RhPessoa pessoa) {
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
            select l.codigo_ccm
              from dbo_uni_pessoas.logradouros_unificado lu
              join dbo_uni_pessoas.logradouros l
                on l.cidade = lu.cidade_correios
               and l.distrito = lu.distrito_correios
               and l.logradouro = lu.logradouro_correios
             where lu.cidade_rh = :cidade
               and lu.distrito_rh = :distrito
               and lu.logradouro_rh = :logradouro
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
            select b.codigo_ccm
              from dbo_uni_pessoas.bairros_unificado bu
              join dbo_uni_pessoas.bairros b
                on b.cidade = bu.cidade_correios
               and b.distrito = bu.distrito_correios
               and b.bairro = bu.bairro_correios
             where bu.cidade_rh = :cidade
               and bu.distrito_rh = :distrito
               and bu.bairro_rh = :bairro
        """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .setParameter("bairro", bairro)
                    .getSingleResult();

            end.bairroId = resultBairro == null ? null : ((Number) resultBairro).longValue();

        } catch (Exception e) {
            end.bairroId = null;
        }

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

    private EnderecoCarga buscarEnderecoPorCepRh(RhPessoa pessoa, Long numero) {
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

    private void buscarCepPorBairroLogradouroNumeroRh(RhPessoa pessoa, EnderecoCarga end) {
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

    private void buscarCepPorTabelaRh(Long cidade, Long distrito, Long logradouro, EnderecoCarga end) {
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

    private boolean cepInvalido(Long cep) {
        if (cep == null) {
            return true;
        }

        return cep < 10000000L || cep > 99999999L || cep == 38100000L;
    }

    private static class EnderecoCarga {
        private Long bairroId;
        private Long logradouroId;
        private Long cepId;
    }
}