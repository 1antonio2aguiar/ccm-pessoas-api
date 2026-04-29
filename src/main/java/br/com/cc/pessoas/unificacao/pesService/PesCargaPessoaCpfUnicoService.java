package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import org.springframework.stereotype.Service;

@Service
public class PesCargaPessoaCpfUnicoService extends PesCargaPessoaUnicoBaseService {

    @Override
    protected String getFisicaJuridica() {
        return "F";
    }

    @Override
    protected Long getTipoEndereco() {
        return 0L;
    }

    @Override
    protected void processarPessoa(PesPessoa pessoa) {
        Long idPessoa = getNextVal("SEQ_CAD_UNICO_PESSOA");
        Long cidadeNascimentoCcm = buscarCidadeNascimentoCcm(pessoa.getCidadeNascimento());
        Integer estadoCivil = mapearEstadoCivil(pessoa.getEstadoCivil());
        String cpf = normalizarCpf(pessoa.getCgcCpf());
        Long tipoPessoa = getTipoPessoa(pessoa);

        inserirCadUnicoPessoaFisica(idPessoa, pessoa, tipoPessoa, cpf, cidadeNascimentoCcm);
        inserirPessoa(idPessoa, pessoa, tipoPessoa);
        inserirDadosPf(idPessoa, pessoa, cpf, estadoCivil, cidadeNascimentoCcm);
        inserirEnderecoPrincipal(idPessoa, pessoa);
        inserirDocumentosPessoaFisica(idPessoa, pessoa);
        inserirContatos(pessoa, idPessoa);
    }

    private void inserirCadUnicoPessoaFisica(
            Long idPessoa,
            PesPessoa pessoa,
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
                .setParameter("id", idPessoa)
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
                .setParameter("observacao", pessoa.getObservacao())
                .setParameter("email", pessoa.getEmail())
                .setParameter("banco", "PESSOAS")
                .setParameter("pessoasCdUnico", idPessoa)
                .executeUpdate();
    }

    private void inserirDadosPf(
            Long idPessoa,
            PesPessoa pessoa,
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
                :localNascimento_id,
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
                .setParameter("localNascimento_id", cidadeNascimentoCcm)
                .setParameter("mae", pessoa.getMae())
                .setParameter("pai", pessoa.getPai())
                .setParameter("dataNascimento", pessoa.getDataNascimento())
                .executeUpdate();
    }

    private void inserirDocumentosPessoaFisica(Long idPessoa, PesPessoa pessoa) {
        if (pessoa.getNumeroDocto() != null) {
            Long tipoDocumento = pessoa.getPesTipoDocumento() != null
                    ? pessoa.getPesTipoDocumento().getTipoDocumento()
                    : 1L;

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
                    :pessoa_id,
                    :tipoDocumento,
                    :numeroDocumento,
                    :orgaoExpedidor,
                    :dataExpedicao
                )
            """)
                    .setParameter("pessoa_id", idPessoa)
                    .setParameter("tipoDocumento", tipoDocumento)
                    .setParameter("numeroDocumento", pessoa.getNumeroDocto())
                    .setParameter("orgaoExpedidor", pessoa.getOrgaoDocto())
                    .setParameter("dataExpedicao", pessoa.getEmissaoDocto())
                    .executeUpdate();
        }

        if (pessoa.getTituloEleitoral() != null) {
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
                    :pessoa_id,
                    :tipoDocumento,
                    :numeroDocumento,
                    :zona,
                    :secao
                )
            """)
                    .setParameter("pessoa_id", idPessoa)
                    .setParameter("tipoDocumento", 11L)
                    .setParameter("numeroDocumento", pessoa.getTituloEleitoral())
                    .setParameter("zona", pessoa.getZona())
                    .setParameter("secao", pessoa.getSecao())
                    .executeUpdate();
        }
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
}
