package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.PesTipoDocumento;
import br.com.cc.pessoas.unificacao.pesEntity.PesTipoPessoa;
import br.com.cc.pessoas.unificacao.pesRepository.PesPessoaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class PesCargaPessoaDuplicadoBaseService {

    @Autowired
    protected PesPessoaRepository pesPessoaRepository;

    @PersistenceContext
    protected EntityManager manager;

    protected abstract String getFisicaJuridica();

    protected abstract Integer getTipoEndereco();

    protected abstract String normalizarCpfCnpj(Long cgcCpf);

    protected abstract void insertDadosEspecificos(Long idPessoa, List<PesPessoa> grupo, DadosGrupo dadosGrupo);

    protected abstract void insertDocumentosEspecificos(Long idPessoa, List<PesPessoa> grupo);

    protected Object getCadDataNascimento(PesPessoa pessoaOrigem, DadosGrupo dadosGrupo) {
        return pessoaOrigem.getDataNascimento();
    }

    protected Object getCadEstadoCivil(PesPessoa pessoaOrigem, DadosGrupo dadosGrupo) {
        return pessoaOrigem.getEstadoCivil();
    }

    protected Object getCadSexo(PesPessoa pessoaOrigem, DadosGrupo dadosGrupo) {
        return pessoaOrigem.getSexo();
    }

    protected Object getCadCidadeNascimento(PesPessoa pessoaOrigem, DadosGrupo dadosGrupo) {
        return dadosGrupo.cidadeNascimentoCcm;
    }

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

    protected void processarGrupo(List<PesPessoa> grupo) {
        PesPessoa principal = grupo.get(0);

        Long idPessoa = getNextVal("DBO_CCM_PESSOAS.SEQ_PESSOAS");

        DadosGrupo dadosGrupo = montarDadosGrupo(grupo);

        insertPessoaPrincipal(
                idPessoa,
                dadosGrupo.nomePrincipal,
                principal.getFisicaJuridica(),
                dadosGrupo.tipoPessoa,
                dadosGrupo.dataCadastro,
                dadosGrupo.observacaoAgrupada
        );

        insertDadosEspecificos(idPessoa, grupo, dadosGrupo);

        for (PesPessoa pessoaOrigem : grupo) {
            insertCadUnicoPessoa(idPessoa, pessoaOrigem, dadosGrupo);
        }

        insertEnderecosUnicos(idPessoa, grupo);
        insertDocumentosEspecificos(idPessoa, grupo);
        insertContatos(idPessoa, grupo);
    }

    protected DadosGrupo montarDadosGrupo(List<PesPessoa> grupo) {
        DadosGrupo dados = new DadosGrupo();
        dados.cpfCnpjPrincipal = buscarCpfCnpjPrincipal(grupo);
        dados.tipoPessoa = buscarPrimeiroTipoPessoa(grupo);
        dados.nomePrincipal = buscarPrimeiroNome(grupo);
        dados.observacaoAgrupada = montarObservacaoAgrupada(grupo);
        dados.cidadeNascimentoCcm = buscarPrimeiraCidadeNascimento(grupo);
        dados.estadoCivil = buscarPrimeiroEstadoCivil(grupo);
        dados.sexo = buscarPrimeiroSexo(grupo);
        dados.nomeSocial = buscarPrimeiroNomeSocial(grupo);
        dados.mae = buscarPrimeiraMae(grupo);
        dados.pai = buscarPrimeiroPai(grupo);
        dados.dataNascimento = buscarPrimeiraDataNascimentoValida(grupo);
        dados.dataCadastro = buscarPrimeiraDataCadastroValida(grupo);
        dados.emailPrincipal = buscarPrimeiroEmail(grupo);
        dados.nomeFantasia = buscarPrimeiraFantasia(grupo);
        dados.objetoSocial = buscarPrimeiroObjetoSocial(grupo);
        dados.microEmpresa = buscarPrimeiroMicroEmpresa(grupo);
        dados.conjugue = buscarPrimeiroConjugue(grupo);
        dados.tipoEmpresa = buscarPrimeiroTipoEmpresa(grupo);
        return dados;
    }

    @SuppressWarnings("unchecked")
    protected List<PesPessoa> buscarGrupoDuplicado(Long pessoaIdSelecionada) {
        List<Number> ids = manager.createNativeQuery("""
            select p.pessoa
              from dbo_ccm_pessoas.pes_pessoas p
             where p.fisica_juridica = :fisicaJuridica
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
                .setParameter("fisicaJuridica", getFisicaJuridica())
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

    protected void insertPessoaPrincipal(Long idPessoa, String nome, String fisicaJuridica, Long tipoPessoa, Object dataCadastro, String observacaoAgrupada) {
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

    protected void insertCadUnicoPessoa(Long idPessoaUnificado, PesPessoa pessoaOrigem, DadosGrupo dadosGrupo) {
        Long tipoPessoa = pessoaOrigem.getPesTipoPessoa() != null
                ? pessoaOrigem.getPesTipoPessoa().getTipoPessoa()
                : 1L;

        String cpfCnpj = normalizarCpfCnpj(pessoaOrigem.getCgcCpf());

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
                .setParameter("cpfCnpj", cpfCnpj == null ? null : Long.valueOf(cpfCnpj))
                .setParameter("dataNascimento", getCadDataNascimento(pessoaOrigem, dadosGrupo))
                .setParameter("estadoCivil", getCadEstadoCivil(pessoaOrigem, dadosGrupo))
                .setParameter("sexo", getCadSexo(pessoaOrigem, dadosGrupo))
                .setParameter("cidadeNascimento", getCadCidadeNascimento(pessoaOrigem, dadosGrupo))
                .setParameter("observacao", pessoaOrigem.getObservacao())
                .setParameter("email", dadosGrupo.emailPrincipal != null ? dadosGrupo.emailPrincipal : pessoaOrigem.getEmail())
                .setParameter("banco", "PESSOAS")
                .setParameter("pessoasCdUnico", idPessoaUnificado)
                .executeUpdate();
    }

    protected void insertEnderecosUnicos(Long idPessoa, List<PesPessoa> grupo) {
        Set<String> enderecosJaInseridos = new HashSet<>();
        boolean principalJaDefinido = false;

        for (PesPessoa pessoa : grupo) {
            EnderecoCarga endereco = null;

            endereco = resolverEnderecoFixo(pessoa);

            if (endereco == null || endereco.logradouroId == null || endereco.bairroId == null) {
                endereco = buscarEnderecoPorMapeamentoPessoa(pessoa);
            }

            if (endereco != null && endereco.logradouroId != null && endereco.bairroId != null && endereco.cepId == null) {
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

            if (endereco == null || endereco.logradouroId == null || endereco.bairroId == null) {
                endereco = buscarEnderecoPorCep(pessoa, pessoa.getNumero());
            }

            if (endereco == null || endereco.logradouroId == null || endereco.bairroId == null) {
                continue;
            }

            String numero = pessoa.getNumero() != null ? pessoa.getNumero().toString().trim() : "0";
            String chave = endereco.bairroId + "|" + endereco.logradouroId + "|" + numero;

            if (enderecosJaInseridos.contains(chave)) {
                continue;
            }

            enderecosJaInseridos.add(chave);

            String principal = !principalJaDefinido ? "S" : "N";

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
                    .setParameter("banco", "PESSOAS")
                    .setParameter("principal", principal)
                    .executeUpdate();

            principalJaDefinido = true;
        }
    }

    protected void insertContatos(Long idPessoa, List<PesPessoa> grupo) {
        Set<String> contatosJaInseridos = new HashSet<>();

        for (PesPessoa pessoa : grupo) {
            if (pessoa.getTelefone() != null && pessoa.getTelefone() != 0) inserirContatoSeNaoExistir(idPessoa, 0L, pessoa.getTelefone(), contatosJaInseridos);
            if (pessoa.getCelular() != null && pessoa.getCelular() != 0) inserirContatoSeNaoExistir(idPessoa, 1L, pessoa.getCelular(), contatosJaInseridos);
            if (pessoa.getRecado() != null && pessoa.getRecado() != 0) inserirContatoSeNaoExistir(idPessoa, 5L, pessoa.getRecado(), contatosJaInseridos);
            if (pessoa.getFax() != null && pessoa.getFax() != 0) inserirContatoSeNaoExistir(idPessoa, 6L, pessoa.getFax(), contatosJaInseridos);
            if (pessoa.getEmail() != null && !pessoa.getEmail().isBlank()) inserirContatoSeNaoExistir(idPessoa, 3L, pessoa.getEmail(), contatosJaInseridos);
            if (pessoa.getPaginaWeb() != null && !pessoa.getPaginaWeb().isBlank()) inserirContatoSeNaoExistir(idPessoa, 4L, pessoa.getPaginaWeb(), contatosJaInseridos);
        }
    }

    protected void inserirContatoSeNaoExistir(Long idPessoa, Long tipoContato, Object contato, Set<String> contatosJaInseridos) {
        String contatoNormalizado = normalizarContato(tipoContato, contato);
        if (contatoNormalizado == null || contatoNormalizado.isBlank()) return;

        String chave = tipoContato + "|" + contatoNormalizado;
        if (contatosJaInseridos.contains(chave)) return;

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

    protected String normalizarContato(Long tipoContato, Object contato) {
        if (contato == null) return null;
        String valor = String.valueOf(contato).trim();
        if (valor.isBlank()) return null;

        if (tipoContato.equals(0L) || tipoContato.equals(1L) || tipoContato.equals(5L) || tipoContato.equals(6L)) {
            String digitos = valor.replaceAll("\\D", "");
            return digitos.isBlank() ? null : digitos;
        }

        if (tipoContato.equals(3L) || tipoContato.equals(4L)) {
            return valor.toLowerCase().trim();
        }

        return valor;
    }

    protected String montarObservacaoAgrupada(List<PesPessoa> grupo) {
        StringBuilder sb = new StringBuilder();

        for (PesPessoa p : grupo) {
            if (p.getObservacao() != null && !p.getObservacao().isBlank()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(p.getObservacao().trim());
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }

    protected String buscarCpfCnpjPrincipal(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            String cpfCnpj = normalizarCpfCnpj(p.getCgcCpf());
            if (cpfCnpj != null && !cpfCnpj.isBlank()) return cpfCnpj;
        }
        return null;
    }

    protected Long buscarPrimeiroTipoPessoa(List<PesPessoa> grupo) {
        return 1L;
    }

    protected String buscarPrimeiroNome(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getNome() != null && !p.getNome().isBlank()) return p.getNome();
        }
        return null;
    }

    protected Object buscarPrimeiraDataNascimentoValida(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getDataNascimento() != null) return p.getDataNascimento();
        }
        return null;
    }

    protected Object buscarPrimeiraDataCadastroValida(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getDataCadastro() != null) return p.getDataCadastro();
        }
        return null;
    }

    protected String buscarPrimeiroEmail(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getEmail() != null && !p.getEmail().isBlank()) return p.getEmail();
        }
        return null;
    }

    protected Long buscarPrimeiraCidadeNascimento(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            Long valor = buscarCidadeNascimentoCcm(p.getCidadeNascimento());
            if (valor != null) return valor;
        }
        return null;
    }

    protected Long buscarCidadeNascimentoCcm(Long cidadeNascimento) {
        if (cidadeNascimento == null || cidadeNascimento == 0L) return null;

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

    protected Integer buscarPrimeiroEstadoCivil(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            Integer valor = mapearEstadoCivil(p.getEstadoCivil());
            if (valor != null) return valor;
        }
        return null;
    }

    protected Integer mapearEstadoCivil(String estadoCivil) {
        if (estadoCivil == null || estadoCivil.isBlank()) return null;

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

    protected String buscarPrimeiroSexo(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getSexo() != null && !p.getSexo().isBlank()) return p.getSexo();
        }
        return null;
    }

    protected String buscarPrimeiroNomeSocial(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getNomeSocial() != null && !p.getNomeSocial().isBlank()) return p.getNomeSocial();
        }
        return null;
    }

    protected String buscarPrimeiraMae(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getMae() != null && !p.getMae().isBlank()) return p.getMae();
        }
        return null;
    }

    protected String buscarPrimeiroPai(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getPai() != null && !p.getPai().isBlank()) return p.getPai();
        }
        return null;
    }

    protected String buscarPrimeiraFantasia(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getFantasia() != null && !p.getFantasia().isBlank()) return p.getFantasia();
        }
        return null;
    }

    protected String buscarPrimeiroObjetoSocial(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getObjetoSocial() != null && !p.getObjetoSocial().isBlank()) return p.getObjetoSocial();
        }
        return null;
    }

    protected String buscarPrimeiroMicroEmpresa(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getMicroEmpresa() != null && !p.getMicroEmpresa().isBlank()) return p.getMicroEmpresa();
        }
        return null;
    }

    protected String buscarPrimeiroConjugue(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getConjugue() != null && !p.getConjugue().isBlank()) return p.getConjugue();
        }
        return null;
    }

    protected Long buscarPrimeiroTipoEmpresa(List<PesPessoa> grupo) {
        for (PesPessoa p : grupo) {
            if (p.getTipoEmpresa() != null) return p.getTipoEmpresa();
        }
        return null;
    }

    protected Long getNextVal(String sequence) {
        return ((Number) manager
                .createNativeQuery("select " + sequence + ".nextval from dual")
                .getSingleResult()).longValue();
    }

    protected EnderecoCarga resolverEnderecoFixo(PesPessoa pessoa) {
        if (pessoa == null) return null;

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
                                Long.valueOf(8888L).equals(logradouro) ||
                                Long.valueOf(9999L).equals(logradouro) ||
                                Long.valueOf(9999L).equals(distrito) ||
                                Long.valueOf(8888L).equals(distrito)
                        );

        if (!enderecoEspecial) return null;

        Long codigoCcm = buscarCodigoCcmDistrito(cidade, distrito);
        if (codigoCcm == null) return null;

        EnderecoCarga end = new EnderecoCarga();
        end.logradouroId = buscarLogradouroPorNome(codigoCcm, logradouroNome);
        end.bairroId = buscarBairroPorNome(codigoCcm, bairroNome);
        end.cepId = 1354012L;

        return end;
    }

    protected EnderecoCarga buscarEnderecoPorCep(PesPessoa pessoa, Long numero) {
        if (pessoa.getCep() == null || cepInvalido(pessoa.getCep())) return null;

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

            if (rows == null || rows.isEmpty()) return null;

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

    protected EnderecoCarga buscarEnderecoPorMapeamentoPessoa(PesPessoa pessoa) {
        if (pessoa == null) return null;

        Long cidade = pessoa.getCidade();
        Long distrito = pessoa.getDistrito();
        Long logradouro = pessoa.getLogradouro();
        Long bairro = pessoa.getBairro();

        if (cidade == null || distrito == null) return null;

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

        if (end.logradouroId == null && end.bairroId == null) return null;

        if (end.logradouroId != null && end.bairroId != null) {
            try {
                Long numero = pessoa.getNumero();
                if (numero == null || numero == 0L) numero = 1L;

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

            if (end.cepId == null || end.cepId == 0L) {
                try {
                    Object resultCepPessoa = manager.createNativeQuery("""
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

                    Long cepPessoa = resultCepPessoa == null ? null : ((Number) resultCepPessoa).longValue();

                    if (cepPessoa != null) {
                        try {
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
                                    .setParameter("cep", cepPessoa)
                                    .getSingleResult();

                            end.cepId = resultCepId == null ? null : ((Number) resultCepId).longValue();
                        } catch (Exception ex) {
                            end.cepId = null;
                        }
                    }
                } catch (Exception e) {
                    end.cepId = null;
                }
            }
        }

        return end;
    }

    protected Long buscarCepIdCompativel(Long cep, Long bairroId, Long logradouroId, Long numero) {
        if (cep == null || cepInvalido(cep) || bairroId == null || logradouroId == null) return null;

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

            if (rows == null || rows.isEmpty()) return null;

            Object row = rows.get(0);
            return row != null ? ((Number) row).longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    protected Long buscarCodigoCcmDistrito(Long cidade, Long distrito) {
        if (cidade == null || distrito == null) return null;

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

    protected Long buscarLogradouroPorNome(Long distritoId, String logradouroNome) {
        if (distritoId == null || logradouroNome == null || logradouroNome.isBlank()) return null;

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
        if (distritoId == null || bairroNome == null || bairroNome.isBlank()) return null;

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

    protected static final Set<Long> CEPS_INVALIDOS_FIXOS = Set.of(38100000L);

    protected boolean cepInvalido(Long cep) {
        if (cep == null) return true;
        return cep < 10000000L || cep > 99999999L || CEPS_INVALIDOS_FIXOS.contains(cep);
    }

    protected Long num(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();

        String s = String.valueOf(o).trim();
        if (s.isEmpty()) return null;

        return Long.valueOf(s);
    }

    protected String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    protected java.time.LocalDateTime ldt(Object o) {
        if (o == null) return null;
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (o instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
        return null;
    }

    protected static class EnderecoCarga {
        protected Long bairroId;
        protected Long logradouroId;
        protected Long cepId;
    }

    protected static class DadosGrupo {
        protected String cpfCnpjPrincipal;
        protected Long tipoPessoa;
        protected String nomePrincipal;
        protected String observacaoAgrupada;
        protected Long cidadeNascimentoCcm;
        protected Integer estadoCivil;
        protected String sexo;
        protected String nomeSocial;
        protected String mae;
        protected String pai;
        protected Object dataNascimento;
        protected Object dataCadastro;
        protected String emailPrincipal;
        protected String nomeFantasia;
        protected String objetoSocial;
        protected String microEmpresa;
        protected String conjugue;
        protected Long tipoEmpresa;
    }
}
