package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PesCargaPessoaCnpjDuplicadoService extends PesCargaPessoaDuplicadoBaseService {

    @Override
    protected String getFisicaJuridica() {
        return "J";
    }

    @Override
    protected Integer getTipoEndereco() {
        return 2;
    }

    @Override
    protected String normalizarCpfCnpj(Long cgcCpf) {
        return normalizarCnpj(cgcCpf);
    }

    @Override
    protected Object getCadDataNascimento(PesPessoa pessoaOrigem, DadosGrupo dadosGrupo) {
        return null;
    }

    @Override
    protected Object getCadEstadoCivil(PesPessoa pessoaOrigem, DadosGrupo dadosGrupo) {
        return null;
    }

    @Override
    protected Object getCadSexo(PesPessoa pessoaOrigem, DadosGrupo dadosGrupo) {
        return null;
    }

    @Override
    protected Object getCadCidadeNascimento(PesPessoa pessoaOrigem, DadosGrupo dadosGrupo) {
        return null;
    }

    @Override
    protected void insertDadosEspecificos(Long idPessoa, List<PesPessoa> grupo, DadosGrupo dadosGrupo) {
        insertDadosPjPrincipal(
                idPessoa,
                dadosGrupo.cpfCnpjPrincipal,
                dadosGrupo.nomeFantasia,
                dadosGrupo.objetoSocial,
                dadosGrupo.microEmpresa,
                dadosGrupo.conjugue,
                dadosGrupo.tipoEmpresa
        );
    }

    @Override
    protected void insertDocumentosEspecificos(Long idPessoa, List<PesPessoa> grupo) {
        insertDocumentosPj(idPessoa, grupo);
    }

    private void insertDadosPjPrincipal(
            Long idPessoa,
            String cnpj,
            String nomeFantasia,
            String objetoSocial,
            String microEmpresa,
            String conjugue,
            Long tipoEmpresa
    ) {
        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.DADOS_PJ
            (
                ID,
                CNPJ,
                NOME_FANTASIA,
                OBJETO_SOCIAL,
                MICRO_EMPRESA,
                CONJUGE,
                TIPO_EMPRESA
            )
            values
            (
                :id,
                :cnpj,
                :nomeFantasia,
                :objetoSocial,
                :microEmpresa,
                :conjugue,
                :tipoEmpresa
            )
        """)
                .setParameter("id", idPessoa)
                .setParameter("cnpj", cnpj)
                .setParameter("nomeFantasia", nomeFantasia)
                .setParameter("objetoSocial", objetoSocial)
                .setParameter("microEmpresa", microEmpresa)
                .setParameter("conjugue", conjugue)
                .setParameter("tipoEmpresa", tipoEmpresa)
                .executeUpdate();
    }

    private void insertDocumentosPj(Long idPessoa, List<PesPessoa> grupo) {
        Set<String> documentosJaInseridos = new HashSet<>();

        for (PesPessoa pessoa : grupo) {
            if (pessoa.getInsricaoEstadual() == null || pessoa.getInsricaoEstadual().isBlank()) {
                continue;
            }

            String inscricaoEstadual = pessoa.getInsricaoEstadual().trim();
            String chaveDocumento = "2|" + inscricaoEstadual.toUpperCase();

            if (documentosJaInseridos.contains(chaveDocumento)) {
                continue;
            }

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
                    .setParameter("tipoDocumento", 2L)
                    .setParameter("numeroDocumento", inscricaoEstadual)
                    .setParameter("orgaoExpedidor", null)
                    .setParameter("dataExpedicao", null)
                    .executeUpdate();
        }
    }

    private String normalizarCnpj(Long cgcCpf) {
        if (cgcCpf == null) {
            return null;
        }

        String cnpj = String.valueOf(cgcCpf).trim();
        int length = cnpj.length();

        if (length < 14) {
            StringBuilder zeros = new StringBuilder();
            for (int i = 0; i < 14 - length; i++) {
                zeros.append('0');
            }
            return zeros + cnpj;
        }

        if (length == 14) {
            return cnpj;
        }

        if (length > 14) {
            return cnpj.substring(length - 14);
        }

        return cnpj;
    }
}
