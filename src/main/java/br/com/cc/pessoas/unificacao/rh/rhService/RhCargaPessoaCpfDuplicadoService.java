package br.com.cc.pessoas.unificacao.rh.rhService;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhPessoa;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RhCargaPessoaCpfDuplicadoService extends RhCargaPessoaUnicoBaseService {

    @Override
    protected String getFisicaJuridica() {
        return "F";
    }

    @Override
    protected Long getTipoEndereco() {
        return 0L;
    }

    @Override
    @Transactional
    protected void processarPessoa(RhPessoa pessoa) {
        List<RhPessoa> grupo = buscarGrupoDuplicadoRh(pessoa);

        if (grupo == null || grupo.isEmpty()) {
            throw new RuntimeException("Grupo duplicado RH não encontrado para pessoa: " + pessoa.getPessoa());
        }

        processarGrupoDuplicado(grupo);
    }

    private void processarGrupoDuplicado(List<RhPessoa> grupo) {
        RhPessoa principal = grupo.get(0);

        Long idPessoa = getNextVal("DBO_CCM_PESSOAS.SEQ_CAD_UNICO_PESSOA");

        String cpf = buscarCpfPrincipal(grupo);
        Long tipoPessoa = getTipoPessoaRh(principal);
        Long cidadeNascimentoCcm = buscarPrimeiraCidadeNascimento(grupo);
        Integer estadoCivil = buscarPrimeiroEstadoCivil(grupo);

        inserirPessoaRh(idPessoa, principal, tipoPessoa);
        inserirDadosPfRh(idPessoa, principal, cpf, estadoCivil, cidadeNascimentoCcm);

        for (RhPessoa origem : grupo) {
            inserirCadUnicoPessoaFisicaRh(
                    idPessoa,
                    origem,
                    getTipoPessoaRh(origem),
                    cpf,
                    cidadeNascimentoCcm
            );
        }

        inserirEnderecosUnicosRh(idPessoa, grupo);
        inserirDocumentosUnicosRh(idPessoa, grupo);
        inserirContatosUnicosRh(idPessoa, grupo);
    }

    @SuppressWarnings("unchecked")
    private List<RhPessoa> buscarGrupoDuplicadoRh(RhPessoa pessoa) {
        List<Number> ids = manager.createNativeQuery("""
            select p.pessoa
              from dbo_rh.pessoas p
             where p.fisica_juridica = 'F'
               and p.cgc_cpf = :cpf
               and replace(transf_caracte(p.nome), ' ', '') =
                   replace(transf_caracte(:nome), ' ', '')
               and not exists (
                    select 1
                      from dbo_ccm_pessoas.cad_unico_pessoa cup
                     where cup.cd_origem = p.pessoa
                       and cup.banco = 'RH'
               )
             order by p.nome, p.cgc_cpf, p.pessoa
        """)
                .setParameter("cpf", pessoa.getCgcCpf())
                .setParameter("nome", pessoa.getNome())
                .getResultList();

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> idsLong = ids.stream()
                .map(Number::longValue)
                .toList();

        return rhPessoaRepository.findAllById(idsLong);
    }

    private void inserirCadUnicoPessoaFisicaRh(
            Long idPessoaUnificado,
            RhPessoa pessoa,
            Long tipoPessoa,
            String cpf,
            Long cidadeNascimentoCcm
    ) {
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
                .setParameter("tipoPessoa", tipoPessoa)
                .setParameter("nome", upper(pessoa.getNome()))
                .setParameter("fisicaJuridica", pessoa.getFisicaJuridica())
                .setParameter("dataCadastro", pessoa.getDataCadastro())
                .setParameter("cpfCnpj", cpf == null ? null : Long.valueOf(cpf))
                .setParameter("dataNascimento", pessoa.getDataNascimento())
                .setParameter("estadoCivil", pessoa.getEstadoCivil())
                .setParameter("sexo", pessoa.getSexo())
                .setParameter("cidadeNascimento", cidadeNascimentoCcm)
                .setParameter("email", pessoa.getEmail())
                .setParameter("banco", "RH")
                .setParameter("pessoasCdUnico", idPessoaUnificado)
                .executeUpdate();
    }

    private void inserirDadosPfRh(
            Long idPessoa,
            RhPessoa pessoa,
            String cpf,
            Integer estadoCivil,
            Long cidadeNascimentoCcm
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
                .setParameter("nomeSocial", buscarPrimeiroNomeSocial(List.of(pessoa)))
                .setParameter("sexo", pessoa.getSexo())
                .setParameter("estadoCivil", estadoCivil)
                .setParameter("localNascimentoId", cidadeNascimentoCcm)
                .setParameter("mae", pessoa.getMae())
                .setParameter("pai", pessoa.getPai())
                .setParameter("dataNascimento", pessoa.getDataNascimento())
                .executeUpdate();
    }

    private void inserirEnderecosUnicosRh(Long idPessoa, List<RhPessoa> grupo) {
        Set<String> enderecosJaInseridos = new HashSet<>();
        boolean principalJaDefinido = false;

        for (RhPessoa pessoa : grupo) {
            EnderecoCarga endereco = resolverEnderecoFixoRh(pessoa);

            if (endereco == null) {
                endereco = buscarEnderecoPorMapeamentoRh(pessoa);
            }

            if (endereco == null || endereco.bairroId == null || endereco.logradouroId == null) {
                endereco = buscarEnderecoPorCepRh(pessoa, pessoa.getNumero());
            }

            if (endereco == null || endereco.bairroId == null || endereco.logradouroId == null) {
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
                    .setParameter("banco", "RH")
                    .setParameter("principal", principal)
                    .executeUpdate();

            principalJaDefinido = true;
        }
    }

    private void inserirDocumentosUnicosRh(Long idPessoa, List<RhPessoa> grupo) {
        Set<String> documentosJaInseridos = new HashSet<>();

        for (RhPessoa pessoa : grupo) {
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
                String chave = tipoDocumento + "|" + numeroDocumento.toUpperCase();

                if (!documentosJaInseridos.contains(chave)) {
                    documentosJaInseridos.add(chave);

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

            if (pessoa.getTituloEleitoral() != null && pessoa.getTituloEleitoral() != 0L) {
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
                            6,
                            :numeroDocumento,
                            :zona,
                            :secao
                        )
                    """)
                            .setParameter("pessoaId", idPessoa)
                            .setParameter("numeroDocumento", pessoa.getTituloEleitoral())
                            .setParameter("zona", pessoa.getZona())
                            .setParameter("secao", pessoa.getSecao())
                            .executeUpdate();
                }
            }
        }
    }

    private void inserirContatosUnicosRh(Long idPessoa, List<RhPessoa> grupo) {
        Set<String> contatosJaInseridos = new HashSet<>();

        for (RhPessoa pessoa : grupo) {
            inserirContatoUnico(idPessoa, 0L, montarTelefone(pessoa.getDddTelefone(), pessoa.getTelefone()), contatosJaInseridos);
            inserirContatoUnico(idPessoa, 1L, montarTelefone(pessoa.getDddCelular(), pessoa.getCelular()), contatosJaInseridos);
            inserirContatoUnico(idPessoa, 1L, pessoa.getWhatsapp(), contatosJaInseridos);
            inserirContatoUnico(idPessoa, 3L, pessoa.getEmail(), contatosJaInseridos);
            inserirContatoUnico(idPessoa, 4L, pessoa.getPaginaWeb(), contatosJaInseridos);
            inserirContatoUnico(idPessoa, 4L, pessoa.getInstagram(), contatosJaInseridos);
            inserirContatoUnico(idPessoa, 4L, pessoa.getFacebook(), contatosJaInseridos);
            inserirContatoUnico(idPessoa, 5L, montarTelefone(pessoa.getDddRecado(), pessoa.getRecado()), contatosJaInseridos);
            inserirContatoUnico(idPessoa, 6L, pessoa.getFax(), contatosJaInseridos);
        }
    }

    private void inserirContatoUnico(
            Long idPessoa,
            Long tipoContato,
            Object contato,
            Set<String> contatosJaInseridos
    ) {
        String contatoNormalizado = normalizarContato(tipoContato, contato);

        if (contatoNormalizado == null || contatoNormalizado.isBlank()) {
            return;
        }

        String chave = tipoContato + "|" + contatoNormalizado;

        if (contatosJaInseridos.contains(chave)) {
            return;
        }

        contatosJaInseridos.add(chave);

        inserirContato(idPessoa, tipoContato, contatoNormalizado);
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

    private String buscarCpfPrincipal(List<RhPessoa> grupo) {
        for (RhPessoa p : grupo) {
            String cpf = normalizarCpf(p.getCgcCpf());

            if (cpf != null && !cpf.isBlank()) {
                return cpf;
            }
        }

        return null;
    }

    private Long buscarPrimeiraCidadeNascimento(List<RhPessoa> grupo) {
        for (RhPessoa p : grupo) {
            Long cidade = buscarCidadeNascimentoCcmRh(p.getCidadeNascimento());

            if (cidade != null) {
                return cidade;
            }
        }

        return null;
    }

    private Integer buscarPrimeiroEstadoCivil(List<RhPessoa> grupo) {
        for (RhPessoa p : grupo) {
            Integer estadoCivil = mapearEstadoCivil(p.getEstadoCivil());

            if (estadoCivil != null) {
                return estadoCivil;
            }
        }

        return null;
    }

    private String buscarPrimeiroNomeSocial(List<RhPessoa> grupo) {
        for (RhPessoa p : grupo) {
            if (p.getNomeSocial() != null && !p.getNomeSocial().isBlank()) {
                return p.getNomeSocial();
            }
        }

        return null;
    }

    private String normalizarCpf(Long cgcCpf) {
        if (cgcCpf == null) {
            return null;
        }

        String cpf = String.valueOf(cgcCpf).trim();

        if (cpf.length() < 11) {
            return "0".repeat(11 - cpf.length()) + cpf;
        }

        if (cpf.length() > 11) {
            return cpf.substring(0, 11);
        }

        return cpf;
    }

    private Integer mapearEstadoCivil(String estadoCivil) {
        if (estadoCivil == null || estadoCivil.isBlank()) {
            return null;
        }

        return switch (estadoCivil.trim()) {
            case "1" -> 3;
            case "2" -> 1;
            case "3" -> 7;
            case "4" -> 2;
            case "5" -> 5;
            case "6" -> 6;
            case "7" -> 8;
            case "8" -> 4;
            default -> null;
        };
    }
}