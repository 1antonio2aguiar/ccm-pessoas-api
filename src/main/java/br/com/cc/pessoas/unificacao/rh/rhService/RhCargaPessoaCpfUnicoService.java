package br.com.cc.pessoas.unificacao.rh.rhService;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhPessoa;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RhCargaPessoaCpfUnicoService extends RhCargaPessoaUnicoBaseService {

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

        Long idPessoa = getNextVal("DBO_CCM_PESSOAS.SEQ_CAD_UNICO_PESSOA");

        String cpf = normalizarCpf(pessoa.getCgcCpf());
        Long tipoPessoa = getTipoPessoaRh(pessoa);
        Long cidadeNascimentoCcm = buscarCidadeNascimentoCcmRh(pessoa.getCidadeNascimento());
        Integer estadoCivil = mapearEstadoCivil(pessoa.getEstadoCivil());

        inserirCadUnicoPessoaFisicaRh(idPessoa, pessoa, tipoPessoa, cpf, cidadeNascimentoCcm);
        inserirPessoaRh(idPessoa, pessoa, tipoPessoa);
        inserirDadosPfRh(idPessoa, pessoa, cpf, estadoCivil, cidadeNascimentoCcm);
        inserirEnderecoPrincipalRh(idPessoa, pessoa);
        inserirDocumentosPessoaFisicaRh(idPessoa, pessoa);
        inserirContatosRh(idPessoa, pessoa);
    }

    // ===============================
    // CAD_UNICO_PESSOA
    // ===============================
    private void inserirCadUnicoPessoaFisicaRh(
            Long idPessoa,
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
                .setParameter("email", pessoa.getEmail())
                .setParameter("banco", "RH")
                .setParameter("pessoasCdUnico", idPessoa)
                .executeUpdate();
    }

    // ===============================
    // DADOS_PF
    // ===============================
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
                .setParameter("nomeSocial", pessoa.getNomeSocial())
                .setParameter("sexo", pessoa.getSexo())
                .setParameter("estadoCivil", estadoCivil)
                .setParameter("localNascimentoId", cidadeNascimentoCcm)
                .setParameter("mae", pessoa.getMae())
                .setParameter("pai", pessoa.getPai())
                .setParameter("dataNascimento", pessoa.getDataNascimento())
                .executeUpdate();
    }

    // ===============================
    // DOCUMENTOS
    // ===============================
    private void inserirDocumentosPessoaFisicaRh(Long idPessoa, RhPessoa pessoa) {

        // Documento principal
        if (pessoa.getNumeroDocto() != null && !pessoa.getNumeroDocto().isBlank()) {

            Long tipoDocumento = pessoa.getRhTipoDocumento() != null
                    ? Long.valueOf(pessoa.getRhTipoDocumento().getTipoDocumento())
                    : 5L;

            if(tipoDocumento == 2 ){
                tipoDocumento = 0L;
            }
            if(tipoDocumento == 15 ){
                tipoDocumento = 5L;
            }

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
                    .setParameter("numeroDocumento", pessoa.getNumeroDocto())
                    .setParameter("orgaoExpedidor", pessoa.getOrgaoDocto())
                    .setParameter("dataExpedicao", pessoa.getEmissaoDocto())
                    .executeUpdate();
        }

        // Título eleitoral
        if (pessoa.getTituloEleitoral() != null && pessoa.getTituloEleitoral() != 0L) {

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

    // ===============================
    // HELPERS CPF
    // ===============================
    private String normalizarCpf(Long cgcCpf) {
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

    private Integer mapearEstadoCivil(String estadoCivil) {
        if (estadoCivil == null || estadoCivil.isBlank()) return null;

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