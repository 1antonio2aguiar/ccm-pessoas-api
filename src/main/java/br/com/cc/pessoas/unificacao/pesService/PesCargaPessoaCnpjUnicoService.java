package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import org.springframework.stereotype.Service;

@Service
public class PesCargaPessoaCnpjUnicoService extends PesCargaPessoaUnicoBaseService {

    @Override
    protected String getFisicaJuridica() {
        return "J";
    }

    @Override
    protected Long getTipoEndereco() {
        return 2L;
    }

    @Override
    protected void processarPessoa(PesPessoa pessoa) {
        Long idPessoa = getNextVal("SEQ_CAD_UNICO_PESSOA");
        String cnpj = normalizarCnpj(pessoa.getCgcCpf());
        Long tipoPessoa = getTipoPessoa(pessoa);

        inserirCadUnicoPessoaJuridica(idPessoa, pessoa, tipoPessoa, cnpj);
        inserirPessoa(idPessoa, pessoa, tipoPessoa);
        inserirDadosPj(idPessoa, pessoa, cnpj);
        inserirEnderecoPrincipal(idPessoa, pessoa);
        inserirDocumentosPessoaJuridica(idPessoa, pessoa);
        inserirContatos(pessoa, idPessoa);
    }

    private void inserirCadUnicoPessoaJuridica(
            Long idPessoa,
            PesPessoa pessoa,
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
                .setParameter("cpfCnpj", cnpj == null ? null : Long.valueOf(cnpj))
                .setParameter("dataNascimento", null)
                .setParameter("estadoCivil", null)
                .setParameter("sexo", null)
                .setParameter("cidadeNascimento", null)
                .setParameter("observacao", pessoa.getObservacao())
                .setParameter("email", pessoa.getEmail())
                .setParameter("banco", "PESSOAS")
                .setParameter("pessoasCdUnico", idPessoa)
                .executeUpdate();
    }

    private void inserirDadosPj(Long idPessoa, PesPessoa pessoa, String cnpj) {
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
                .setParameter("nomeFantasia", pessoa.getFantasia())
                .setParameter("objetoSocial", pessoa.getObjetoSocial())
                .setParameter("microEmpresa", pessoa.getMicroEmpresa())
                .setParameter("conjugue", pessoa.getConjugue())
                .setParameter("tipoEmpresa", pessoa.getTipoEmpresa())
                .executeUpdate();
    }

    private void inserirDocumentosPessoaJuridica(Long idPessoa, PesPessoa pessoa) {
        if (pessoa.getInsricaoEstadual() != null) {
            inserirDocumentoSimples(idPessoa, 2L, pessoa.getInsricaoEstadual());
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
