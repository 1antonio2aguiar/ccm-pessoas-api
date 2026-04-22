package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.entity.enuns.TipoDocumento;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.PesTipoDocumento;
import br.com.cc.pessoas.unificacao.pesEntity.PesTipoPessoa;
import br.com.cc.pessoas.unificacao.pesRepository.PesPessoaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PesCargaPessoaCpfDuplicadoService {

    @Autowired
    private PesPessoaRepository pesPessoaRepository;

    @PersistenceContext
    private EntityManager manager;

    @Transactional
    public void processarPessoaUnica(Long pessoaId) {
        if (pessoaId == null) {
            throw new RuntimeException("Pessoa não informada.");
        }

        List<PesPessoa> grupo = buscarGrupoDuplicado(pessoaId);

        if (grupo == null || grupo.isEmpty()) {
            throw new RuntimeException("Nenhum grupo duplicado encontrado para a pessoa " + pessoaId);
        }

        processarGrupo(grupo);
    }

    private void processarGrupo(List<PesPessoa> grupo) {
        PesPessoa principal = grupo.get(0);

        Long idPessoa = getNextVal("DBO_CCM_PESSOAS.SEQ_PESSOAS");

        String cpfPrincipal = buscarCpfPrincipal(grupo);
        Long tipoPessoa = buscarPrimeiroTipoPessoa(grupo);
        String nomePrincipal = buscarPrimeiroNome(grupo);
        String observacaoAgrupada = montarObservacaoAgrupada(grupo);

        Long cidadeNascimentoCcm = buscarPrimeiraCidadeNascimento(grupo);
        Integer estadoCivil = buscarPrimeiroEstadoCivil(grupo);
        String sexo = buscarPrimeiroSexo(grupo);
        String nomeSocial = buscarPrimeiroNomeSocial(grupo);
        String mae = buscarPrimeiraMae(grupo);
        String pai = buscarPrimeiroPai(grupo);
        Object dataNascimento = buscarPrimeiraDataNascimentoValida(grupo);
        Object dataCadastro = buscarPrimeiraDataCadastroValida(grupo);
        String emailPrincipal = buscarPrimeiroEmail(grupo);

        insertPessoaPrincipal(
                idPessoa,
                nomePrincipal,
                principal.getFisicaJuridica(),
                tipoPessoa,
                dataCadastro,
                observacaoAgrupada
        );

        insertDadosPfPrincipal(
                idPessoa,
                cpfPrincipal,
                nomeSocial,
                sexo,
                estadoCivil,
                cidadeNascimentoCcm,
                mae,
                pai,
                dataNascimento
        );

        for (PesPessoa pessoaOrigem : grupo) {
            insertCadUnicoPessoa(
                    idPessoa,
                    pessoaOrigem,
                    cidadeNascimentoCcm,
                    emailPrincipal
            );
        }

        insertEnderecosUnicos(idPessoa, grupo);
        insertDocumentos(idPessoa, grupo);
        insertContatos(idPessoa, grupo);
    }

    @SuppressWarnings("unchecked")
    private List<PesPessoa> buscarGrupoDuplicado(Long pessoaIdSelecionada) {
        List<Number> ids = manager.createNativeQuery("""
        select p.pessoa
          from dbo_ccm_pessoas.pes_pessoas p
         where p.fisica_juridica = 'F'
           and p.cgc_cpf = (
                select p0.cgc_cpf
                  from dbo_ccm_pessoas.pes_pessoas p0
                 where p0.pessoa = :pessoaId
           )
           and replace(transf_caracte(p.nome), ' ', '') = (
                select replace(transf_caracte(p1.nome), ' ', '')
                  from dbo_ccm_pessoas.pes_pessoas p1
                 where p1.pessoa = :pessoaId
           )
           and not exists (
                select 1
                  from dbo_ccm_pessoas.cad_unico_pessoa cup
                 where cup.cd_origem = p.pessoa
           )
         order by p.nome, p.cgc_cpf, p.pessoa
    """)
                .setParameter("pessoaId", pessoaIdSelecionada)
                .getResultList();

        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> idsLong = ids.stream().map(Number::longValue).toList();

        List<Object[]> rows = manager.createNativeQuery("""
        select
            p.pessoa,
            p.nome,
            p.fisica_juridica,
            p.data_cadastro,
            p.cgc_cpf,
            p.tipo_pessoa,
            p.cidade,
            p.distrito,
            p.bairro,
            p.logradouro,
            p.numero,
            p.complemento,
            p.cep,
            p.data_nascimento,
            p.estado_civil,
            p.sexo,
            p.cidade_nascimento,
            p.pais,
            p.tipo_documento,
            p.numero_docto,
            p.orgao_docto,
            p.emissao_docto,
            p.titulo_eleitoral,
            p.zona,
            p.secao,
            p.mae,
            p.pai,
            p.telefone,
            p.recado,
            p.celular,
            p.fax,
            p.e_mail,
            p.pagina_web,
            p.pessoa_matriz,
            p.inscricao_estadual,
            p.fantasia,
            p.profissao,
            p.vip,
            p.observacao,
            p.conjugue,
            p.objeto_social,
            p.microempresa,
            p.mes_envio_sicom,
            p.ano_envio_sicom,
            p.tipo_empresa,
            p.nome_social,
            p.deficiente
        from dbo_ccm_pessoas.pes_pessoas p
        where p.pessoa in (:ids)
        order by p.nome, p.cgc_cpf, p.pessoa
    """)
                .setParameter("ids", idsLong)
                .getResultList();

        List<PesPessoa> lista = new ArrayList<>();

        for (Object[] r : rows) {
            PesPessoa p = new PesPessoa();

            p.setPessoa(num(r[0]));
            p.setNome(str(r[1]));
            p.setFisicaJuridica(str(r[2]));
            p.setDataCadastro(ldt(r[3]));
            p.setCgcCpf(num(r[4]));

            if (r[5] != null) {
                PesTipoPessoa tipoPessoa = new PesTipoPessoa();
                tipoPessoa.setTipoPessoa(num(r[5]).intValue());
                p.setPesTipoPessoa(tipoPessoa);
            }

            p.setCidade(num(r[6]));
            p.setDistrito(num(r[7]));
            p.setBairro(num(r[8]));
            p.setLogradouro(num(r[9]));
            p.setNumero(num(r[10]));
            p.setComplemento(str(r[11]));
            p.setCep(num(r[12]));
            p.setDataNascimento(ldt(r[13]));
            p.setEstadoCivil(str(r[14]));
            p.setSexo(str(r[15]));
            p.setCidadeNascimento(num(r[16]));
            p.setPais(num(r[17]));

            if (r[18] != null) {
                PesTipoDocumento tipoDocumento = new PesTipoDocumento();
                tipoDocumento.setTipoDocumento(num(r[18]).longValue());
                p.setPesTipoDocumento(tipoDocumento);
            }

            p.setNumeroDocto(str(r[19]));
            p.setOrgaoDocto(str(r[20]));
            p.setEmissaoDocto(ldt(r[21]));
            p.setTituloEleitoral(num(r[22]));
            p.setZona(num(r[23]));
            p.setSecao(num(r[24]));
            p.setMae(str(r[25]));
            p.setPai(str(r[26]));
            p.setTelefone(num(r[27]));
            p.setRecado(num(r[28]));
            p.setCelular(num(r[29]));
            p.setFax(num(r[30]));
            p.setEmail(str(r[31]));
            p.setPaginaWeb(str(r[32]));
            p.setPessoaMatriz(num(r[33]));
            p.setInsricaoEstadual(str(r[34]));
            p.setFantasia(str(r[35]));
            p.setProfissao(num(r[36]));
            p.setVip(str(r[37]));
            p.setObservacao(str(r[38]));
            p.setConjugue(str(r[39]));
            p.setObjetoSocial(str(r[40]));
            p.setMicroEmpresa(str(r[41]));
            p.setMesEnvioSicom(num(r[42]));
            p.setAnoEnvioSicom(num(r[43]));
            p.setTipoEmpresa(num(r[44]));
            p.setNomeSocial(str(r[45]));
            p.setDeficiente(str(r[46]));

            lista.add(p);
        }

        return lista;
    }

    private void insertPessoaPrincipal(
            Long idPessoa,
            String nome,
            String fisicaJuridica,
            Long tipoPessoa,
            Object dataCadastro,
            String observacaoAgrupada
    ) {
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
                :tipoPessoaId,
                :nome,
                :dataCadastro,
                :fisicaJuridica,
                :observacao,
                :situacaoId
            )
        """)
                .setParameter("id", idPessoa)
                .setParameter("tipoPessoaId", tipoPessoa)
                .setParameter("nome", nome != null ? nome.toUpperCase().trim() : null)
                .setParameter("dataCadastro", dataCadastro)
                .setParameter("fisicaJuridica", fisicaJuridica)
                .setParameter("observacao", observacaoAgrupada)
                .setParameter("situacaoId", 1L)
                .executeUpdate();
    }

    private void insertDadosPfPrincipal(
            Long idPessoa,
            String cpf,
            String nomeSocial,
            String sexo,
            Integer estadoCivil,
            Long localNascimentoId,
            String mae,
            String pai,
            Object dataNascimento
    ) {
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
                .setParameter("nomeSocial", nomeSocial)
                .setParameter("sexo", sexo)
                .setParameter("estadoCivil", estadoCivil)
                .setParameter("localNascimentoId", localNascimentoId)
                .setParameter("mae", mae)
                .setParameter("pai", pai)
                .setParameter("dataNascimento", dataNascimento)
                .executeUpdate();
    }

    private void insertCadUnicoPessoa(
            Long idPessoaUnificado,
            PesPessoa pessoaOrigem,
            Long cidadeNascimentoCcm,
            String emailPrincipal
    ) {
        Long tipoPessoa = pessoaOrigem.getPesTipoPessoa() != null
                ? pessoaOrigem.getPesTipoPessoa().getTipoPessoa()
                : 1L;

        String cpf = normalizarCpf(pessoaOrigem.getCgcCpf());

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
                .setParameter("id", getNextVal("DBO_CCM_PESSOAS.SEQ_CAD_UNICO_PESSOA"))
                .setParameter("cdOrigem", pessoaOrigem.getPessoa())
                .setParameter("tipoPessoa", tipoPessoa)
                .setParameter("nome", pessoaOrigem.getNome() != null ? pessoaOrigem.getNome().toUpperCase().trim() : null)
                .setParameter("fisicaJuridica", pessoaOrigem.getFisicaJuridica())
                .setParameter("dataCadastro", pessoaOrigem.getDataCadastro())
                .setParameter("cpfCnpj", cpf == null ? null : Long.valueOf(cpf))
                .setParameter("dataNascimento", pessoaOrigem.getDataNascimento())
                .setParameter("estadoCivil", pessoaOrigem.getEstadoCivil())
                .setParameter("sexo", pessoaOrigem.getSexo())
                .setParameter("cidadeNascimento", cidadeNascimentoCcm)
                .setParameter("observacao", pessoaOrigem.getObservacao())
                .setParameter("email", emailPrincipal != null ? emailPrincipal : pessoaOrigem.getEmail())
                .setParameter("banco", "PESSOAS")
                .setParameter("pessoasCdUnico", idPessoaUnificado)
                .executeUpdate();
    }

    private void insertEnderecosUnicos(Long idPessoa, List<PesPessoa> grupo) {

        Set<String> enderecosJaInseridos = new HashSet<>();
        boolean principalJaDefinido = false;

        for (PesPessoa pessoa : grupo) {

            EnderecoCarga endereco = null;

            try {
                // 1) tenta endereço especial/fixo
                endereco = resolverEnderecoFixo(pessoa);

                // 2) tenta pelos códigos da própria pessoa, MESMO COM CEP VÁLIDO
                if (endereco == null || endereco.logradouroId == null || endereco.bairroId == null) {
                    endereco = buscarEnderecoPorMapeamentoPessoa(pessoa);
                }

                // 3) se conseguiu bairro/logradouro pelos códigos, tenta apenas achar um CEP compatível
                if (endereco != null && endereco.logradouroId != null && endereco.bairroId != null) {
                    Long cepIdCompativel = buscarCepIdCompativel(
                            pessoa.getCep(),
                            endereco.bairroId,
                            endereco.logradouroId,
                            pessoa.getNumero()
                    );

                    if (cepIdCompativel != null) {
                        endereco.cepId = cepIdCompativel;
                    }
                }

                // 4) fallback final: se não conseguiu pelos códigos, aí sim tenta pelo CEP
                if (endereco == null || endereco.logradouroId == null || endereco.bairroId == null) {
                    endereco = buscarEnderecoPorCep(pessoa, pessoa.getNumero());
                }

                if (endereco == null || endereco.logradouroId == null || endereco.bairroId == null) {
                    continue;
                }

                String numero = pessoa.getNumero() != null
                        ? pessoa.getNumero().toString().trim()
                        : "0";

                String chave = endereco.bairroId + "|" + endereco.logradouroId + "|" + numero;

                if (enderecosJaInseridos.contains(chave)) {
                    continue;
                }

                enderecosJaInseridos.add(chave);

                String principal = !principalJaDefinido ? "S" : "N";
                Integer tipoEndereco = 0;
                String banco = "PESSOAS";

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
                        .setParameter("tipoEndereco", tipoEndereco)
                        .setParameter("bairroId", endereco.bairroId)
                        .setParameter("logradouroId", endereco.logradouroId)
                        .setParameter("numero", pessoa.getNumero())
                        .setParameter("complemento", pessoa.getComplemento())
                        .setParameter("cepId", endereco.cepId)
                        .setParameter("banco", banco)
                        .setParameter("principal", principal)
                        .executeUpdate();

                principalJaDefinido = true;

            } catch (Exception e) {
                if (endereco == null) {
                    System.err.println("endereco = null");
                } else {
                    System.err.println("bairroId: " + endereco.bairroId);
                    System.err.println("logradouroId: " + endereco.logradouroId);
                    System.err.println("cepId: " + endereco.cepId);
                }

                e.printStackTrace();
                throw e;
            }
        }
    }

    private void insertDocumentos(Long idPessoa, List<PesPessoa> grupo) {
        Set<String> documentosJaInseridos = new HashSet<>();

        for (PesPessoa pessoa : grupo) {

            if (pessoa.getNumeroDocto() != null && !pessoa.getNumeroDocto().isBlank()) {

                Integer tipoDocumentoNormalizado = pessoa.getPesTipoDocumento() != null
                        ? normalizarTipoDocumento(pessoa.getPesTipoDocumento().getTipoDocumento())
                        : null;

                if (tipoDocumentoNormalizado != null) {
                    Long tipoDocumento = tipoDocumentoNormalizado.longValue();

                    String numeroDocumento = pessoa.getNumeroDocto().trim();
                    String chaveDocumento = tipoDocumento + "|" + numeroDocumento.toUpperCase();

                    if (!documentosJaInseridos.contains(chaveDocumento)) {
                        documentosJaInseridos.add(chaveDocumento);

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
                                .setParameter("pessoaId", idPessoa)
                                .setParameter("tipoDocumento", tipoDocumento)
                                .setParameter("numeroDocumento", numeroDocumento)
                                .setParameter("orgaoExpedidor", pessoa.getOrgaoDocto())
                                .setParameter("dataExpedicao", pessoa.getEmissaoDocto())
                                .executeUpdate();
                    }
                }
            }

            if (pessoa.getTituloEleitoral() != null && pessoa.getTituloEleitoral() != 0) {
                String chaveTitulo = "6|" + pessoa.getTituloEleitoral();

                if (!documentosJaInseridos.contains(chaveTitulo)) {
                    documentosJaInseridos.add(chaveTitulo);

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
                            .setParameter("pessoaId", idPessoa)
                            .setParameter("tipoDocumento", 6L)
                            .setParameter("numeroDocumento", pessoa.getTituloEleitoral())
                            .setParameter("zona", pessoa.getZona())
                            .setParameter("secao", pessoa.getSecao())
                            .executeUpdate();
                }
            }
        }
    }

    private void insertContatos(Long idPessoa, List<PesPessoa> grupo) {
        Set<String> contatosJaInseridos = new HashSet<>();

        for (PesPessoa pessoa : grupo) {

            if (pessoa.getTelefone() != null && pessoa.getTelefone() != 0) {
                inserirContatoSeNaoExistir(idPessoa, 0L, pessoa.getTelefone(), contatosJaInseridos);
            }

            if (pessoa.getCelular() != null && pessoa.getCelular() != 0) {
                inserirContatoSeNaoExistir(idPessoa, 1L, pessoa.getCelular(), contatosJaInseridos);
            }

            if (pessoa.getRecado() != null && pessoa.getRecado() != 0) {
                inserirContatoSeNaoExistir(idPessoa, 5L, pessoa.getRecado(), contatosJaInseridos);
            }

            if (pessoa.getFax() != null && pessoa.getFax() != 0) {
                inserirContatoSeNaoExistir(idPessoa, 6L, pessoa.getFax(), contatosJaInseridos);
            }

            if (pessoa.getEmail() != null && !pessoa.getEmail().isBlank()) {
                inserirContatoSeNaoExistir(idPessoa, 3L, pessoa.getEmail(), contatosJaInseridos);
            }

            if (pessoa.getPaginaWeb() != null && !pessoa.getPaginaWeb().isBlank()) {
                inserirContatoSeNaoExistir(idPessoa, 4L, pessoa.getPaginaWeb(), contatosJaInseridos);
            }
        }
    }

    private void inserirContatoSeNaoExistir(Long idPessoa, Long tipoContato, Object contato, Set<String> contatosJaInseridos) {
        String contatoNormalizado = normalizarContato(tipoContato, contato);

        if (contatoNormalizado == null || contatoNormalizado.isBlank()) {
            return;
        }

        String chave = tipoContato + "|" + contatoNormalizado;

        if (contatosJaInseridos.contains(chave)) {
            return;
        }

        contatosJaInseridos.add(chave);

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
                .setParameter("contato", contatoNormalizado)
                .executeUpdate();
    }

    private String normalizarContato(Long tipoContato, Object contato) {
        if (contato == null) {
            return null;
        }

        String valor = String.valueOf(contato).trim();

        if (valor.isBlank()) {
            return null;
        }

        // telefone, celular, recado, fax
        if (tipoContato.equals(0L) || tipoContato.equals(1L) || tipoContato.equals(5L) || tipoContato.equals(6L)) {
            String digitos = valor.replaceAll("\\D", "");
            return digitos.isBlank() ? null : digitos;
        }

        // email
        if (tipoContato.equals(3L)) {
            return valor.toLowerCase().trim();
        }

        // página web
        if (tipoContato.equals(4L)) {
            return valor.toLowerCase().trim();
        }

        return valor;
    }

    private String montarObservacaoAgrupada(List<PesPessoa> grupo) {
        StringBuilder sb = new StringBuilder();

        for (PesPessoa p : grupo) {
            if (p.getObservacao() != null && !p.getObservacao().isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(p.getObservacao().trim());
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }

    private String buscarCpfPrincipal(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            String cpf = normalizarCpf(p.getCgcCpf());
            if (cpf != null && !cpf.isBlank()) {
                return cpf;
            }
        }
        return null;
    }
    private Long buscarPrimeiroTipoPessoa(List<PesPessoa> grupo) {

        return 1L;
    }

    private String buscarPrimeiroNome(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getNome() != null && !p.getNome().isBlank()) {
                return p.getNome();
            }
        }
        return null;
    }

    private Object buscarPrimeiraDataNascimentoValida(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getDataNascimento() != null) {
                return p.getDataNascimento();
            }
        }
        return null;
    }

    private Object buscarPrimeiraDataCadastroValida(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getDataCadastro() != null) {
                return p.getDataCadastro();
            }
        }
        return null;
    }

    private String buscarPrimeiroEmail(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getEmail() != null && !p.getEmail().isBlank()) {
                return p.getEmail();
            }
        }
        return null;
    }

    private Long buscarPrimeiraCidadeNascimento(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            Long valor = buscarCidadeNascimentoCcm(p.getCidadeNascimento());
            if (valor != null) {
                return valor;
            }
        }
        return null;
    }

    private Long buscarCidadeNascimentoCcm(Long cidadeNascimento) {
        if (cidadeNascimento == null || cidadeNascimento == 0L) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
                select d.cidade
                  from dbo_uni_pessoas.distritos d,
                       dbo_uni_pessoas.distritos_unificado du
                 where du.cidade_correios   = d.cidade
                   and du.distrito_correios = d.distrito
                   and du.cidade_pessoa     = :cidadeNascimento
                   and du.distrito_pessoa   = 1
            """)
                    .setParameter("cidadeNascimento", cidadeNascimento)
                    .getSingleResult();

            return result == null ? null : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    private Integer buscarPrimeiroEstadoCivil(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            Integer valor = mapearEstadoCivil(p.getEstadoCivil());
            if (valor != null) {
                return valor;
            }
        }
        return null;
    }

    private Integer mapearEstadoCivil(String estadoCivil) {
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

    private String buscarPrimeiroSexo(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getSexo() != null && !p.getSexo().isBlank()) {
                return p.getSexo();
            }
        }
        return null;
    }

    private String buscarPrimeiroNomeSocial(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getNomeSocial() != null && !p.getNomeSocial().isBlank()) {
                return p.getNomeSocial();
            }
        }
        return null;
    }

    private String buscarPrimeiraMae(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getMae() != null && !p.getMae().isBlank()) {
                return p.getMae();
            }
        }
        return null;
    }

    private String buscarPrimeiroPai(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getPai() != null && !p.getPai().isBlank()) {
                return p.getPai();
            }
        }
        return null;
    }

    private String normalizarCpf(Long cgcCpf) {
        if (cgcCpf == null) {
            return null;
        }

        String cpf = String.valueOf(cgcCpf).trim();
        int length = cpf.length();

        if (length < 11) {
            StringBuilder zeros = new StringBuilder();
            for (int i = 0; i < 11 - length; i++) {
                zeros.append('0');
            }
            return zeros + cpf;
        }

        if (length == 11) {
            return cpf;
        }

        if (length == 12) {
            return cpf.substring(1);
        }

        if (length == 13 || length == 14) {
            if (cpf.equals("77777777777777")) {
                return "77777777777";
            }

            if (cpf.equals("12543307000118")) {
                return "54330700011";
            }

            return "00000000000";
        }

        return cpf;
    }

    private Long getNextVal(String sequence) {
        return ((Number) manager
                .createNativeQuery("select " + sequence + ".nextval from dual")
                .getSingleResult()).longValue();
    }

    private static class EnderecoCarga {
        private Long bairroId;
        private Long logradouroId;
        private Long cepId;
    }

    private EnderecoCarga resolverEnderecoFixo(PesPessoa pessoa) {

        if (pessoa == null) {
            return null;
        }

        Long cidade = pessoa.getCidade();
        Long bairro = pessoa.getBairro();
        Long distrito = pessoa.getDistrito();
        Long logradouro = pessoa.getLogradouro();

        String logradouroNome = pessoa.getLogradouroNome();
        String bairroNome = pessoa.getBairroNome();

        boolean enderecoEspecial =
                (Long.valueOf(1L).equals(cidade) || Long.valueOf(9999L).equals(cidade)) &&
                        (
                                Long.valueOf(8888L).equals(bairro) ||
                                        Long.valueOf(9999L).equals(bairro) ||
                                        Long.valueOf(9999L).equals(logradouro) ||
                                        Long.valueOf(9999L).equals(distrito) ||
                                        Long.valueOf(8888L).equals(distrito)
                        );

        if (!enderecoEspecial) {
            return null;
        }

        Long codigoCcm = buscarCodigoCcmDistrito(cidade, distrito);

        if (codigoCcm == null) {
            return null;
        }

        EnderecoCarga end = new EnderecoCarga();

        end.logradouroId = buscarLogradouroPorNome(codigoCcm, logradouroNome);
        end.bairroId = buscarBairroPorNome(codigoCcm, bairroNome);

        if (Long.valueOf(1L).equals(cidade)) {
            end.cepId = 1354012L;
        } else if (Long.valueOf(9999L).equals(cidade)) {
            end.cepId = 1354012L;
        } else {
            end.cepId = null;
        }

        return end;
    }

    private EnderecoCarga buscarEnderecoPorCep(PesPessoa pessoa, Long numero) {

        if (pessoa.getCep() == null) {
            return null;
        }

        if (cepInvalido(pessoa.getCep())) {
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

    /*private EnderecoCarga buscarEnderecoSemCep(PesPessoa pessoa) {

        if (pessoa == null) {
            return null;
        }

        if (!cepInvalido(pessoa.getCep())) {
            return null;
        }

        return buscarEnderecoPorMapeamentoPessoa(pessoa);
    }*/

    private Long buscarCodigoCcmDistrito(Long cidade, Long distrito) {

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
                   and du.cidade_pessoa = :cidade
                   and du.distrito_pessoa = :distrito
            """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .getSingleResult();

            return result == null ? null : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    private Long buscarLogradouroPorNome(Long distritoId, String logradouroNome) {

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

    private Long buscarBairroPorNome(Long distritoId, String bairroNome) {

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

    private static final Set<Long> CEPS_INVALIDOS_FIXOS = Set.of(
            38100000L
    );

    private boolean cepInvalido(Long cep) {
        if (cep == null) {
            return true;
        }

        return cep < 10000000L || cep > 99999999L || CEPS_INVALIDOS_FIXOS.contains(cep);
    }

    private Integer normalizarTipoDocumento(Long tipoDocumentoOrigem) {
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

            case 8, 11 -> null;

            default -> 9999;
        };
    }

    private EnderecoCarga buscarEnderecoPorMapeamentoPessoa(PesPessoa pessoa) {

        if (pessoa == null) {
            return null;
        }

        Long cidade = pessoa.getCidade();
        Long distrito = pessoa.getDistrito();
        Long logradouro = pessoa.getLogradouro();
        Long bairro = pessoa.getBairro();

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
             where lu.cidade_pessoa = :cidade
               and lu.distrito_pessoa = :distrito
               and lu.logradouro_pessoa = :logradouro
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

            end.bairroId = resultBairro == null ? null : ((Number) resultBairro).longValue();

        } catch (Exception e) {
            end.bairroId = null;
        }

        end.cepId = null;

        if (end.logradouroId == null && end.bairroId == null) {
            return null;
        }

        return end;
    }

    private Long buscarCepIdCompativel(Long cep, Long bairroId, Long logradouroId, Long numero) {

        if (cep == null || cepInvalido(cep) || bairroId == null || logradouroId == null) {
            return null;
        }

        String cepLimpo = String.valueOf(cep).trim();

        try {
            @SuppressWarnings("unchecked")
            List<Object> rows = manager.createNativeQuery("""
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
                     where trim(c.cep) = :cep
                       and c.bairro_id = :bairroId
                       and c.logradouro_id = :logradouroId
                     order by ordem, c.id
                   )
             where rownum = 1
        """)
                    .setParameter("cep", cepLimpo)
                    .setParameter("bairroId", bairroId)
                    .setParameter("logradouroId", logradouroId)
                    .setParameter("numero", numero)
                    .getResultList();

            if (rows == null || rows.isEmpty()) {
                System.err.println("CEP NAO ENCONTRADO PARA: " +
                        " Cep " + cep +
                        " Bairro " + bairroId +
                        " Logradouro " + logradouroId +
                        " Numero " + numero);
                return null;
            }

            Object row = rows.get(0);
            return row != null ? ((Number) row).longValue() : null;

        } catch (Exception e) {
            System.err.println("ERRO AO BUSCAR CEP COMPATIVEL: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Long num(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();

        String s = String.valueOf(o).trim();
        if (s.isEmpty()) return null;

        return Long.valueOf(s);
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private java.time.LocalDateTime ldt(Object o) {
        if (o == null) return null;
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (o instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
        return null;
    }
}