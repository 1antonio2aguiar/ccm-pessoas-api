package br.com.cc.pessoas.unificacao.pessoa.pesService;

import br.com.cc.pessoas.unificacao.controle.ctrlEntity.PesCargaPessoasCtrl;
import br.com.cc.pessoas.unificacao.controle.ctrlEntity.PesCargaPessoasCtrlDto;
import br.com.cc.pessoas.unificacao.controle.ctrlEntity.PesCargaPessoasCtrlRepository;
import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pessoa.pesRepository.PesPessoaRepository;
import br.com.cc.pessoas.entity.enuns.TipoDocumento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class PesPessoaBaseService {

    @Autowired
    protected PesPessoaRepository pesPessoaRepository;

    @Autowired
    protected PesCargaPessoasCtrlRepository cargaCtrlRepository;

    @PersistenceContext
    protected EntityManager manager;

    protected Long getTipoEndereco(PesPessoa pessoa) {

        if (pessoa == null) {
            return 0L;
        }

        return "J".equalsIgnoreCase(pessoa.getFisicaJuridica())
                ? 1L
                : 0L;
    }

    protected Long getNextVal(String sequence) {

        return ((Number) manager
                .createNativeQuery(
                        "select " + sequence + ".nextval from dual")
                .getSingleResult())
                .longValue();
    }

    protected Long getTipoPessoa(PesPessoa pessoa) {

        return pessoa.getPesTipoPessoa() != null
                ? pessoa.getPesTipoPessoa().getTipoPessoa()
                : 1L;
    }

    protected String upper(String valor) {

        return valor == null
                ? null
                : valor.trim().toUpperCase();
    }

    protected Long getCidadeEndereco(PesPessoa pessoa) {

        if (pessoa.getCidade() != null) {
            return pessoa.getCidade();
        }

        return pessoa.getPesCidade() != null
                ? pessoa.getPesCidade().getCidade()
                : null;
    }

    protected Long getDistritoEndereco(PesPessoa pessoa) {

        if (pessoa.getDistrito() != null) {
            return pessoa.getDistrito();
        }

        return pessoa.getPesDistrito() != null
                ? pessoa.getPesDistrito().getDistrito()
                : null;
    }

    protected Long getLogradouroEndereco(PesPessoa pessoa) {

        if (pessoa.getLogradouro() != null) {
            return pessoa.getLogradouro();
        }

        return pessoa.getPesLogradouro() != null
                ? pessoa.getPesLogradouro().getLogradouro()
                : null;
    }

    protected Long getBairroEndereco(PesPessoa pessoa) {

        if (pessoa.getBairro() != null) {
            return pessoa.getBairro();
        }

        return pessoa.getPesBairro() != null
                ? pessoa.getPesBairro().getBairro()
                : null;
    }

    protected void inserirPessoa(
            Long idPessoa,
            PesPessoa pessoa,
            Long tipoPessoa) {

        manager.createNativeQuery("""
                            insert into DBO_CCM_PESSOAS.PESSOAS
                            (
                                ID,
                                TIPO_PESSOA_ID,
                                NOME,
                                DATA_CADASTRO,
                                FISICA_JURIDICA,
                                OBSERVACAO,
                                SITUACAO_ID
                            )
                            values
                            (
                                :id,
                                :tipoPessoa_id,
                                :nome,
                                :dataCadastro,
                                :fisicaJuridica,
                                :observacao,
                                :situacao_id
                            )
                        """)
                .setParameter("id", idPessoa)
                .setParameter("tipoPessoa_id", tipoPessoa)
                .setParameter("nome", upper(pessoa.getNome()))
                .setParameter("dataCadastro", pessoa.getDataCadastro())
                .setParameter("fisicaJuridica", pessoa.getFisicaJuridica())
                .setParameter("observacao", pessoa.getObservacao())
                .setParameter("situacao_id", 1)
                .executeUpdate();
    }

    protected void inserirContato(
            Long idPessoa,
            Long tipoContato,
            Object contato) {

        inserirContato(
                idPessoa,
                tipoContato,
                contato,
                null
        );
    }
    protected void inserirContatos(
            PesPessoa pessoa,
            Long idPessoa) {

        if (pessoa.getTelefone() != null
                && pessoa.getTelefone() != 0) {

            inserirContato(
                    idPessoa,
                    0L,
                    pessoa.getTelefone()
            );
        }

        if (pessoa.getFax() != null) {
            inserirContato(
                    idPessoa,
                    6L,
                    pessoa.getFax()
            );
        }

        if (pessoa.getEmail() != null) {
            inserirContato(
                    idPessoa,
                    3L,
                    pessoa.getEmail()
            );
        }

        if (pessoa.getPaginaWeb() != null) {
            inserirContato(
                    idPessoa,
                    4L,
                    pessoa.getPaginaWeb()
            );
        }

        if (pessoa.getRecado() != null
                && pessoa.getRecado() != 0) {

            inserirContato(
                    idPessoa,
                    5L,
                    pessoa.getRecado()
            );
        }

        if (pessoa.getCelular() != null
                && pessoa.getCelular() != 0) {

            inserirContato(
                    idPessoa,
                    1L,
                    pessoa.getCelular()
            );
        }
    }
    protected void inserirContato(
            Long idPessoa,
            Long tipoContato,
            Object contato,
            Long dadosPjId) {

        manager.createNativeQuery("""
        insert into DBO_CCM_PESSOAS.CONTATOS
        (
            ID,
            PESSOA_ID,
            TIPO_CONTATO,
            CONTATO,
            DADOS_PJ_ID
        )
        values
        (
            SEQ_CONTATOS.nextval,
            :pessoa_id,
            :tipoContato,
            :contato,
            :dadosPjId
        )
        """)
                .setParameter("pessoa_id", idPessoa)
                .setParameter("tipoContato", tipoContato)
                .setParameter("contato", contato)
                .setParameter("dadosPjId", dadosPjId)
                .executeUpdate();
    }

    protected void inserirEnderecoPrincipal(
            Long idPessoa,
            PesPessoa pessoa,
            Long dadosPjId) {

        EnderecoCarga endereco =
                resolverEnderecoCarga(pessoa);

        if (endereco == null) {
            return;
        }
        inserirEnderecoResolvido(
                idPessoa,
                pessoa,
                endereco,
                "S",
                dadosPjId
        );

        /*EnderecoCarga endereco = resolverEnderecoFixo(pessoa);

        if (endereco == null) {
            endereco = buscarEnderecoSemCep(pessoa, 0);
        }

        if (endereco == null) {
            endereco = buscarEnderecoPorCep(
                    pessoa,
                    pessoa.getNumero()
            );

            if (endereco == null) {
                endereco = buscarEnderecoSemCep(pessoa, 1);
            }
        }

        if (endereco == null) {
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
            PRINCIPAL,
            DADOS_PJ_ID
        )
        values
        (
            SEQ_ENDERECOS.nextval,
            :pessoa_id,
            :tipoEndereco,
            :bairro_id,
            :logradouro_id,
            :numero,
            :complemento,
            :cep_id,
            :banco,
            :principal,
            :dados_pj_id
        )
        """)
                .setParameter("pessoa_id", idPessoa)
                .setParameter(
                    "tipoEndereco",
                    dadosPjId != null
                            ? 2L
                            : getTipoEndereco(pessoa)
                )
                .setParameter(
                        "bairro_id",
                        endereco.bairroId
                )
                .setParameter(
                        "logradouro_id",
                        endereco.logradouroId
                )
                .setParameter(
                        "numero",
                        pessoa.getNumero()
                )
                .setParameter(
                        "complemento",
                        pessoa.getComplemento()
                )
                .setParameter(
                        "cep_id",
                        endereco.cepId
                )
                .setParameter("banco", "PESSOAS")
                .setParameter("principal", "S")
                .setParameter("dados_pj_id", dadosPjId)
                .executeUpdate();*/
    }
    protected void inserirEnderecoResolvido(
            Long idPessoa,
            PesPessoa pessoa,
            EnderecoCarga endereco,
            String principal,
            Long dadosPjId) {

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
            PRINCIPAL,
            DADOS_PJ_ID
        )
        values
        (
            SEQ_ENDERECOS.nextval,
            :pessoa_id,
            :tipoEndereco,
            :bairro_id,
            :logradouro_id,
            :numero,
            :complemento,
            :cep_id,
            :banco,
            :principal,
            :dados_pj_id
        )
        """)
                .setParameter("pessoa_id", idPessoa)
                .setParameter(
                        "tipoEndereco",
                        dadosPjId != null
                                ? 2L
                                : getTipoEndereco(pessoa)
                )
                .setParameter(
                        "bairro_id",
                        endereco.bairroId
                )
                .setParameter(
                        "logradouro_id",
                        endereco.logradouroId
                )
                .setParameter(
                        "numero",
                        pessoa.getNumero()
                )
                .setParameter(
                        "complemento",
                        pessoa.getComplemento()
                )
                .setParameter(
                        "cep_id",
                        endereco.cepId
                )
                .setParameter("banco", "PESSOAS")
                .setParameter("principal", principal)
                .setParameter("dados_pj_id", dadosPjId)
                .executeUpdate();
    }
    protected EnderecoCarga resolverEnderecoFixo(
            PesPessoa pessoa) {

        if (pessoa == null) {
            return null;
        }

        Long cidade = getCidadeEndereco(pessoa);
        Long bairro = getBairroEndereco(pessoa);
        Long distrito = getDistritoEndereco(pessoa);
        Long logradouro = getLogradouroEndereco(pessoa);

        String logradouroNome = pessoa.getLogradouroNome();
        String bairroNome = pessoa.getBairroNome();

        boolean enderecoEspecial =
                (
                        Long.valueOf(1L).equals(cidade)
                                || Long.valueOf(9999L).equals(cidade)
                )
                        &&
                        (
                                Long.valueOf(8888L).equals(bairro)
                                        || Long.valueOf(9999L).equals(bairro)
                                        || Long.valueOf(8888L).equals(logradouro)
                                        || Long.valueOf(9999L).equals(logradouro)
                                        || Long.valueOf(8888L).equals(distrito)
                                        || Long.valueOf(9999L).equals(distrito)
                        );

        if (!enderecoEspecial) {
            return null;
        }

        Long codigoCcm = buscarCodigoCcmDistrito(
                cidade,
                distrito
        );

        if (codigoCcm == null) {
            return null;
        }

        EnderecoCarga endereco = new EnderecoCarga();

        endereco.logradouroId = buscarLogradouroPorNome(
                codigoCcm,
                logradouroNome
        );

        endereco.bairroId = buscarBairroPorNome(
                codigoCcm,
                bairroNome
        );

        endereco.cepId = 1354012L;

        return endereco;
    }

    protected EnderecoCarga buscarEnderecoPorCep(
            PesPessoa pessoa,
            Long numero) {

        if (pessoa == null
                || pessoa.getCep() == null
                || cepInvalido(pessoa.getCep())) {

            return null;
        }

        String cepLimpo =
                String.valueOf(pessoa.getCep()).trim();

        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = manager.createNativeQuery("""
                                select id,
                                       bairro_id,
                                       logradouro_id
                                  from (
                                        select c.id,
                                               c.bairro_id,
                                               c.logradouro_id,
                                               case
                                                   when :numero is not null
                                                    and c.numero_ini is not null
                                                    and c.numero_fim is not null
                                                    and :numero between c.numero_ini
                                                                    and c.numero_fim
                                                   then 0
                                                   else 1
                                               end as ordem
                                          from dbo_ccm_pessoas.ceps c
                                         where trim(c.cep) = :cep
                                         order by ordem,
                                                  c.id
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

            EnderecoCarga endereco = new EnderecoCarga();

            endereco.cepId =
                    row[0] == null
                            ? null
                            : ((Number) row[0]).longValue();

            endereco.bairroId =
                    row[1] == null
                            ? null
                            : ((Number) row[1]).longValue();

            endereco.logradouroId =
                    row[2] == null
                            ? null
                            : ((Number) row[2]).longValue();

            return endereco;

        } catch (Exception e) {
            return null;
        }
    }

    protected EnderecoCarga buscarEnderecoSemCep(
            PesPessoa pessoa,
            Integer status) {

        if (pessoa == null) {
            return null;
        }

        if (!cepInvalido(pessoa.getCep())
                && Integer.valueOf(0).equals(status)) {

            return null;
        }

        Long cidade = getCidadeEndereco(pessoa);
        Long distrito = getDistritoEndereco(pessoa);
        Long logradouro = getLogradouroEndereco(pessoa);
        Long bairro = getBairroEndereco(pessoa);

        if (cidade == null || distrito == null) {
            return null;
        }

        EnderecoCarga endereco = new EnderecoCarga();

        try {
            Object resultLogradouro =
                    manager.createNativeQuery("""
                                        select l.codigo_ccm
                                          from dbo_uni_pessoas.logradouros_unificado lu
                                          join dbo_uni_pessoas.logradouros l
                                            on l.cidade = lu.cidade_correios
                                           and l.distrito = lu.distrito_correios
                                           and l.logradouro = lu.logradouro_correios
                                         where lu.cidade_pessoa = :cidade
                                           and lu.distrito_pessoa = :distrito
                                           and lu.logradouro_pessoa = :logradouro
                                    """)
                            .setParameter("cidade", cidade)
                            .setParameter("distrito", distrito)
                            .setParameter("logradouro", logradouro)
                            .getSingleResult();

            endereco.logradouroId =
                    resultLogradouro == null
                            ? null
                            : ((Number) resultLogradouro).longValue();

        } catch (Exception e) {
            endereco.logradouroId = null;
        }

        try {
            Object resultBairro =
                    manager.createNativeQuery("""
                                        select l.codigo_ccm
                                          from dbo_uni_pessoas.bairros_unificado lu
                                          join dbo_uni_pessoas.bairros l
                                            on l.cidade = lu.cidade_correios
                                           and l.distrito = lu.distrito_correios
                                           and l.bairro = lu.bairro_correios
                                         where lu.cidade_pessoa = :cidade
                                           and lu.distrito_pessoa = :distrito
                                           and lu.bairro_pessoa = :bairro
                                    """)
                            .setParameter("cidade", cidade)
                            .setParameter("distrito", distrito)
                            .setParameter("bairro", bairro)
                            .getSingleResult();

            endereco.bairroId =
                    resultBairro == null
                            ? null
                            : ((Number) resultBairro).longValue();

        } catch (Exception e) {
            endereco.bairroId = null;
        }

        endereco.cepId = null;

        if (endereco.logradouroId == null
                && endereco.bairroId == null) {

            return null;
        }

        if (endereco.logradouroId != null
                && endereco.bairroId != null) {

            buscarCepPorBairroLogradouroNumero(
                    pessoa,
                    endereco
            );

            if (endereco.cepId == null
                    || endereco.cepId == 0L) {

                buscarCepPorTabelaPessoas(
                        cidade,
                        distrito,
                        logradouro,
                        endereco
                );
            }
        }

        return endereco;
    }

    protected void buscarCepPorBairroLogradouroNumero(
            PesPessoa pessoa,
            EnderecoCarga endereco) {

        try {
            Long numero = pessoa.getNumero();

            if (numero == null || numero == 0L) {
                numero = 1L;
            }

            Object resultCep =
                    manager.createNativeQuery("""
                                        select id
                                          from (
                                                select c.id,
                                                       case
                                                           when :numero is not null
                                                            and c.numero_ini is not null
                                                            and c.numero_fim is not null
                                                            and :numero between c.numero_ini
                                                                            and c.numero_fim
                                                           then 0
                                                           else 1
                                                       end as ordem
                                                  from dbo_ccm_pessoas.ceps c
                                                 where c.bairro_id = :bairroId
                                                   and c.logradouro_id = :logradouroId
                                                 order by ordem,
                                                          c.id
                                               )
                                         where rownum = 1
                                    """)
                            .setParameter(
                                    "bairroId",
                                    endereco.bairroId
                            )
                            .setParameter(
                                    "logradouroId",
                                    endereco.logradouroId
                            )
                            .setParameter(
                                    "numero",
                                    numero
                            )
                            .getSingleResult();

            endereco.cepId =
                    resultCep == null
                            ? null
                            : ((Number) resultCep).longValue();

        } catch (Exception e) {
            endereco.cepId = null;
        }
    }

    protected void buscarCepPorTabelaPessoas(
            Long cidade,
            Long distrito,
            Long logradouro,
            EnderecoCarga endereco) {

        try {
            Object resultCepPessoa =
                    manager.createNativeQuery("""
                                        select cep
                                          from (
                                                select cep
                                                  from dbo_pessoas.ceps
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

            Long cepPessoa =
                    resultCepPessoa == null
                            ? null
                            : ((Number) resultCepPessoa).longValue();

            if (cepPessoa == null) {
                endereco.cepId = null;
                return;
            }

            Object resultCepId =
                    manager.createNativeQuery("""
                                        select id
                                          from (
                                                select id
                                                  from dbo_ccm_pessoas.ceps
                                                 where cep = :cep
                                                 order by id
                                               )
                                         where rownum = 1
                                    """)
                            .setParameter(
                                    "cep",
                                    cepPessoa
                            )
                            .getSingleResult();

            endereco.cepId =
                    resultCepId == null
                            ? null
                            : ((Number) resultCepId).longValue();

        } catch (Exception e) {
            endereco.cepId = null;
        }
    }

    protected Long buscarCodigoCcmDistrito(
            Long cidade,
            Long distrito) {

        if (cidade == null || distrito == null) {
            return null;
        }

        try {
            Object result =
                    manager.createNativeQuery("""
                                        select d.codigo_ccm
                                          from dbo_uni_pessoas.distritos d,
                                               dbo_uni_pessoas.distritos_unificado du
                                         where d.cidade = du.cidade_correios
                                           and d.distrito = du.distrito_correios
                                           and du.cidade_pessoa = :cidade
                                           and du.distrito_pessoa = :distrito
                                    """)
                            .setParameter("cidade", cidade)
                            .setParameter("distrito", distrito)
                            .getSingleResult();

            return result == null
                    ? null
                    : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    protected Long buscarLogradouroPorNome(
            Long distritoId,
            String logradouroNome) {

        if (distritoId == null
                || logradouroNome == null
                || logradouroNome.isBlank()) {

            return null;
        }

        try {
            Object result =
                    manager.createNativeQuery("""
                                        select id
                                          from dbo_ccm_pessoas.logradouros
                                         where distrito_id = :distritoId
                                           and upper(nome) = upper(:logradouroNome)
                                    """)
                            .setParameter(
                                    "distritoId",
                                    distritoId
                            )
                            .setParameter(
                                    "logradouroNome",
                                    logradouroNome.trim()
                            )
                            .getSingleResult();

            return result == null
                    ? null
                    : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    protected Long buscarBairroPorNome(
            Long distritoId,
            String bairroNome) {

        if (distritoId == null
                || bairroNome == null
                || bairroNome.isBlank()) {

            return null;
        }

        try {
            Object result =
                    manager.createNativeQuery("""
                                        select id
                                          from dbo_ccm_pessoas.bairros
                                         where distrito_id = :distritoId
                                           and upper(nome) = upper(:bairroNome)
                                    """)
                            .setParameter(
                                    "distritoId",
                                    distritoId
                            )
                            .setParameter(
                                    "bairroNome",
                                    bairroNome.trim()
                            )
                            .getSingleResult();

            return result == null
                    ? null
                    : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    protected static final Set<Long> CEPS_INVALIDOS_FIXOS =
            Set.of(
                    38100000L
            );

    protected boolean cepInvalido(Long cep) {

        if (cep == null) {
            return true;
        }

        return cep < 10000000L
                || cep > 99999999L
                || CEPS_INVALIDOS_FIXOS.contains(cep);
    }

    protected static class EnderecoCarga {

        protected Long bairroId;
        protected Long logradouroId;
        protected Long cepId;
    }
    /**********************************************************************
     * PESSOA FÍSICA
     **********************************************************************/

    protected boolean cpfJaExisteNoDadosPf(String cpf) {

        if (cpf == null || cpf.isBlank()) {
            return false;
        }

        Number total = (Number) manager.createNativeQuery("""
            select count(1)
              from DBO_CCM_PESSOAS.DADOS_PF
             where CPF = :cpf
        """)
                .setParameter("cpf", cpf)
                .getSingleResult();

        return total != null && total.longValue() > 0;
    }

    protected void inserirCadUnicoPessoaFisica(
            Long idPessoa,
            PesPessoa pessoa,
            Long tipoPessoa,
            String cpf,
            Long cidadeNascimentoCcm) {

        Long cadUnicoPessoaId = getNextVal("SEQ_CAD_UNICO_PESSOA");

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
                OBSERVACAO,
                EMAIL,
                BANCO,
                PESSOAS_CD_UNICO
            )
            values
            (
                :id,
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
                :observacao,
                :email,
                :banco,
                :pessoasCdUnico
            )
        """)
                .setParameter("id", cadUnicoPessoaId)
                .setParameter("cdOrigem", pessoa.getPessoa())
                .setParameter("tipoPessoa", tipoPessoa)
                .setParameter("nome", upper(pessoa.getNome()))
                .setParameter("fisicaJuridica", pessoa.getFisicaJuridica())
                .setParameter("dataCadastro", pessoa.getDataCadastro())
                .setParameter(
                        "cpfCnpj",
                        cpf == null || cpf.isBlank()
                                ? null
                                : Long.valueOf(cpf)
                )
                .setParameter("dataNascimento", pessoa.getDataNascimento())
                .setParameter("estadoCivil", pessoa.getEstadoCivil())
                .setParameter("sexo", pessoa.getSexo())
                .setParameter("cidadeNascimento", cidadeNascimentoCcm)
                .setParameter("observacao", pessoa.getObservacao())
                .setParameter("email", pessoa.getEmail())
                .setParameter("banco", "PESSOAS")
                .setParameter("pessoasCdUnico", idPessoa)
                .executeUpdate();
    }

    protected void inserirDadosPf(
            Long idPessoa,
            PesPessoa pessoa,
            String cpf,
            Integer estadoCivil,
            Long cidadeNascimentoCcm) {

        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.DADOS_PF
            (
                ID,
                CPF,
                NOME_SOCIAL,
                SEXO,
                ESTADO_CIVIL,
                LOCAL_NASCIMENTO_ID,
                MAE,
                PAI,
                DATA_NASCIMENTO
            )
            values
            (
                :id,
                :cpf,
                :nomeSocial,
                :sexo,
                :estadoCivil,
                :localNascimentoId,
                :mae,
                :pai,
                :dataNascimento
            )
        """)
                .setParameter("id", idPessoa)
                .setParameter("cpf", cpf)
                .setParameter("nomeSocial", pessoa.getNomeSocial())
                .setParameter("sexo", pessoa.getSexo())
                .setParameter("estadoCivil", estadoCivil)
                .setParameter("localNascimentoId", cidadeNascimentoCcm)
                .setParameter("mae", pessoa.getMae())
                .setParameter("pai", pessoa.getPai())
                .setParameter("dataNascimento", pessoa.getDataNascimento())
                .executeUpdate();
    }

    protected void inserirDocumentoSimples(
            Long idPessoa,
            Long tipoDocumento,
            String documento) {

        inserirDocumentoSimples(
                idPessoa,
                tipoDocumento,
                documento,
                null
        );
    }

    protected void inserirDocumentoSimples(
            Long idPessoa,
            Long tipoDocumento,
            String documento,
            Long dadosPjId) {

        manager.createNativeQuery("""
        insert into DBO_CCM_PESSOAS.DOCUMENTOS
        (
            ID,
            PESSOA_ID,
            TIPO_DOCUMENTO,
            NUMERO_DOCUMENTO,
            ORGAO_EXPEDIDOR,
            DATA_EXPEDICAO,
            DADOS_PJ_ID
        )
        values
        (
            SEQ_DOCUMENTOS.nextval,
            :pessoa_id,
            :tipoDocumento,
            :numeroDocumento,
            :orgaoExpedidor,
            :dataExpedicao,
            :dadosPjId
        )
        """)
                .setParameter("pessoa_id", idPessoa)
                .setParameter("tipoDocumento", tipoDocumento)
                .setParameter("numeroDocumento", documento)
                .setParameter("orgaoExpedidor", null)
                .setParameter("dataExpedicao", null)
                .setParameter("dadosPjId", dadosPjId)
                .executeUpdate();
    }

    protected void inserirDocumentosPessoa(
            Long idPessoa,
            PesPessoa pessoa) {

        inserirDocumentosPessoa(
                idPessoa,
                pessoa,
                new HashSet<>()
        );
    }

    protected void inserirDocumentosPessoa(
            Long idPessoa,
            PesPessoa pessoa,
            Set<String> documentosInseridos) {

        Long tipoDocumentoOrigem =
                pessoa.getPesTipoDocumento() != null
                        ? pessoa.getPesTipoDocumento().getTipoDocumento()
                        : null;

        Integer tipoDocumentoDestino =
                normalizarTipoDocumento(
                        tipoDocumentoOrigem
                );

        /*
         * Documento comum.
         */
        if (pessoa.getNumeroDocto() != null
                && !pessoa.getNumeroDocto().isBlank()
                && tipoDocumentoDestino != null) {

            String numeroDocumento =
                    pessoa.getNumeroDocto().trim();

            String chaveDocumento =
                    tipoDocumentoDestino
                            + "|"
                            + numeroDocumento.toUpperCase();

            if (documentosInseridos.add(chaveDocumento)) {

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
                        .setParameter(
                                "pessoaId",
                                idPessoa
                        )
                        .setParameter(
                                "tipoDocumento",
                                tipoDocumentoDestino
                        )
                        .setParameter(
                                "numeroDocumento",
                                numeroDocumento
                        )
                        .setParameter(
                                "orgaoExpedidor",
                                pessoa.getOrgaoDocto()
                        )
                        .setParameter(
                                "dataExpedicao",
                                pessoa.getEmissaoDocto()
                        )
                        .executeUpdate();
            }
        }

        /*
         * Título eleitoral.
         */
        if (pessoa.getTituloEleitoral() != null) {

            Integer tipoTitulo =
                    TipoDocumento.TÍTULOD_DE_ELEITOR.getCodigo();

            String numeroTitulo =
                    String.valueOf(
                            pessoa.getTituloEleitoral()
                    ).trim();

            String chaveTitulo =
                    tipoTitulo + "|" + numeroTitulo;

            if (documentosInseridos.add(chaveTitulo)) {

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
                        .setParameter(
                                "pessoaId",
                                idPessoa
                        )
                        .setParameter(
                                "tipoDocumento",
                                tipoTitulo
                        )
                        .setParameter(
                                "numeroDocumento",
                                pessoa.getTituloEleitoral()
                        )
                        .setParameter(
                                "zona",
                                pessoa.getZona()
                        )
                        .setParameter(
                                "secao",
                                pessoa.getSecao()
                        )
                        .executeUpdate();
            }
        }
    }

    protected String normalizarCpf(
            String cnpjCpf) {

        if (cnpjCpf == null
                || cnpjCpf.isBlank()) {

            return null;
        }

        String cpf = cnpjCpf.trim();

        if (!cpf.matches("\\d+")) {
            throw new RuntimeException(
                    "CPF inválido: contém caracteres não numéricos."
            );
        }

        if (cpf.length() != 11) {
            throw new RuntimeException(
                    "CPF inválido: deve possuir exatamente 11 dígitos."
            );
        }

        return cpf;
    }

    protected Integer mapearEstadoCivil(String estadoCivil) {

        if (estadoCivil == null || estadoCivil.isBlank()) {
            return null;
        }

        return switch (estadoCivil) {
            case "A" -> 0;
            case "C" -> 1;
            case "D" -> 2;
            case "S" -> 3;
            case "U" -> 4;
            case "V" -> 5;
            case "O" -> 6;
            default -> null;
        };
    }

    protected Long buscarCidadeNascimentoCcm(Long cidadeNascimento) {

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
                   and du.cidade_pessoa = :cidadeNascimento
                   and du.distrito_pessoa = 1
            """)
                    .setParameter("cidadeNascimento", cidadeNascimento)
                    .getSingleResult();

            return result == null
                    ? null
                    : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }
    /**********************************************************************
     * CONTROLE DA CARGA
     **********************************************************************/

    public Long iniciarControle() {

        Long idControle = getNextVal(
                "DBO_CCM_PESSOAS.SEQ_CARGA_PESSOAS_CTRL"
        );

        PesCargaPessoasCtrl controle =
                new PesCargaPessoasCtrl();

        controle.setId(idControle);
        controle.setStatus("INICIADO");
        controle.setTotalProcessado(0L);
        controle.setTotalErros(0L);
        controle.setMensagemErro(null);

        cargaCtrlRepository.save(controle);

        return idControle;
    }

    public PesCargaPessoasCtrlDto buscarStatus(
            Long idControle) {

        PesCargaPessoasCtrl controle =
                buscarControle(idControle);

        return new PesCargaPessoasCtrlDto(
                controle.getId(),
                controle.getStatus(),
                controle.getTotalProcessado(),
                controle.getTotalErros(),
                controle.getMensagemErro()
        );
    }

    protected PesCargaPessoasCtrl buscarControle(
            Long idControle) {

        if (idControle == null) {
            throw new IllegalArgumentException(
                    "Código do controle não informado."
            );
        }

        return cargaCtrlRepository.findById(idControle)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Controle não encontrado: "
                                        + idControle
                        )
                );
    }

    protected void marcarIniciado(
            Long idControle) {

        PesCargaPessoasCtrl controle =
                buscarControle(idControle);

        controle.setStatus("PROCESSANDO");
        controle.setDataInicio(LocalDateTime.now());
        controle.setDataFim(null);
        controle.setMensagemErro(null);

        if (controle.getTotalProcessado() == null) {
            controle.setTotalProcessado(0L);
        }

        if (controle.getTotalErros() == null) {
            controle.setTotalErros(0L);
        }

        cargaCtrlRepository.save(controle);
    }

    protected void somarProcessado(
            Long idControle) {

        PesCargaPessoasCtrl controle =
                buscarControle(idControle);

        Long totalAtual =
                controle.getTotalProcessado() == null
                        ? 0L
                        : controle.getTotalProcessado();

        controle.setTotalProcessado(
                totalAtual + 1L
        );

        cargaCtrlRepository.save(controle);
    }

    protected void somarErro(
            Long idControle,
            String mensagem) {

        PesCargaPessoasCtrl controle =
                buscarControle(idControle);

        Long totalAtual =
                controle.getTotalErros() == null
                        ? 0L
                        : controle.getTotalErros();

        controle.setTotalErros(
                totalAtual + 1L
        );

        controle.setMensagemErro(
                normalizarMensagemErro(mensagem)
        );

        cargaCtrlRepository.save(controle);
    }

    protected void marcarFinalizado(
            Long idControle) {

        PesCargaPessoasCtrl controle =
                buscarControle(idControle);

        controle.setStatus("FINALIZADO");
        controle.setDataFim(LocalDateTime.now());

        cargaCtrlRepository.save(controle);
    }

    protected void marcarErroGeral(
            Long idControle,
            String mensagem) {

        PesCargaPessoasCtrl controle =
                buscarControle(idControle);

        controle.setStatus("ERRO");
        controle.setDataFim(LocalDateTime.now());
        controle.setMensagemErro(
                normalizarMensagemErro(mensagem)
        );

        cargaCtrlRepository.save(controle);
    }

    protected String normalizarMensagemErro(
            String mensagem) {

        if (mensagem == null || mensagem.isBlank()) {
            return "Erro não identificado durante o processamento.";
        }

        String mensagemTratada =
                mensagem.trim();

        int tamanhoMaximo = 3900;

        if (mensagemTratada.length() > tamanhoMaximo) {
            return mensagemTratada.substring(
                    0,
                    tamanhoMaximo
            );
        }

        return mensagemTratada;
    }
    protected Integer normalizarTipoDocumento(
            Long tipoDocumentoOrigem) {

        if (tipoDocumentoOrigem == null) {
            return null;
        }

        return switch (tipoDocumentoOrigem.intValue()) {
            case 1 -> TipoDocumento.RG.getCodigo();
            case 2 -> TipoDocumento.CTPS.getCodigo();
            case 3 -> TipoDocumento.CNH.getCodigo();
            case 4 -> TipoDocumento.RESERVISTA.getCodigo();
            case 5 -> TipoDocumento.PASSAPORTE.getCodigo();
            case 6 -> TipoDocumento.CREA.getCodigo();
            case 9 -> TipoDocumento.RG_ESTRANGEIRO.getCodigo();
            case 10 -> TipoDocumento.CRM.getCodigo();

            /*
             * Tipo 8 e tipo 11 não devem ser inseridos
             * como documento comum.
             *
             * O título eleitoral é tratado separadamente.
             */
            case 8, 11 -> null;

            default -> 9999;
        };
    }

    /* APARTIR DAQUI É UMA IMPLEMENTACAO PARA CPF(S) DUPLICADOS. */

    protected EnderecoCarga resolverEnderecoCarga(
            PesPessoa pessoa) {

        EnderecoCarga endereco =
                resolverEnderecoFixo(pessoa);

        if (endereco == null) {
            endereco =
                    buscarEnderecoSemCep(
                            pessoa,
                            0
                    );
        }

        if (endereco == null) {
            endereco =
                    buscarEnderecoPorCep(
                            pessoa,
                            pessoa.getNumero()
                    );

            if (endereco == null) {
                endereco =
                        buscarEnderecoSemCep(
                                pessoa,
                                1
                        );
            }
        }

        return endereco;
    }
}