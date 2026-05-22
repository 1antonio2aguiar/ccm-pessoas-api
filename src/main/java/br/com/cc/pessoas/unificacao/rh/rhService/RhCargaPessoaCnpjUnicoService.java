package br.com.cc.pessoas.unificacao.rh.rhService;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhPessoa;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RhCargaPessoaCnpjUnicoService extends RhCargaPessoaUnicoBaseService {

    @Override
    protected String getFisicaJuridica() {
        return "J";
    }

    @Override
    protected Long getTipoEndereco() {
        return 0L;
    }

    @Override
    @Transactional
    protected void processarPessoa(RhPessoa pessoa) {
        Long idPessoa = getNextVal("DBO_CCM_PESSOAS.SEQ_CAD_UNICO_PESSOA");

        String cnpj = normalizarCnpj(pessoa.getCgcCpf());
        Long tipoPessoa = getTipoPessoaRh(pessoa);

        inserirCadUnicoPessoaJuridicaRh(idPessoa, pessoa, tipoPessoa, cnpj);
        inserirPessoaRh(idPessoa, pessoa, tipoPessoa);
        inserirDadosPjRh(idPessoa, pessoa, cnpj);
        inserirEnderecoPrincipalRh(idPessoa, pessoa);
        inserirDocumentosPessoaJuridicaRh(idPessoa, pessoa);
        inserirContatosRh(idPessoa, pessoa);
    }

    private void inserirCadUnicoPessoaJuridicaRh(
            Long idPessoa,
            RhPessoa pessoa,
            Long tipoPessoa,
            String cnpj
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
                .setParameter("cpfCnpj", cnpj == null ? null : Long.valueOf(cnpj))
                .setParameter("email", pessoa.getEmail())
                .setParameter("banco", "RH")
                .setParameter("pessoasCdUnico", idPessoa)
                .executeUpdate();
    }

    private void inserirDadosPjRh(Long idPessoa, RhPessoa pessoa, String cnpj) {
        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.DADOS_PJ
            (
                ID,
                CNPJ,
                NOME_FANTASIA
            )
            values
            (
                :id,
                :cnpj,
                :nomeFantasia
            )
        """)
                .setParameter("id", idPessoa)
                .setParameter("cnpj", cnpj)
                .setParameter("nomeFantasia", pessoa.getFantasia())
                .executeUpdate();
    }

    private void inserirDocumentosPessoaJuridicaRh(Long idPessoa, RhPessoa pessoa) {
        if (pessoa.getNumeroDocto() == null || pessoa.getNumeroDocto().isBlank()) {
            return;
        }

        Long tipoDocumento = pessoa.getRhTipoDocumento() != null
                ? Long.valueOf(pessoa.getRhTipoDocumento().getTipoDocumento())
                : 5L;

        if (tipoDocumento == 2L) {
            tipoDocumento = 0L;
        }

        if (tipoDocumento == 15L) {
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

    private String normalizarCnpj(Long cgcCpf) {
        if (cgcCpf == null) {
            return null;
        }

        String cnpj = String.valueOf(cgcCpf).trim();

        if (cnpj.length() < 14) {
            return "0".repeat(14 - cnpj.length()) + cnpj;
        }

        if (cnpj.length() > 14) {
            return cnpj.substring(0, 14);
        }

        return cnpj;
    }
}