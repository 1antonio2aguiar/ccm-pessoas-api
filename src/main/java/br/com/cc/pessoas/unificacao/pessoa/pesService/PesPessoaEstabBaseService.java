package br.com.cc.pessoas.unificacao.pessoa.pesService;

import br.com.cc.pessoas.unificacao.pessoa.pesDto.PesEmpresaVinculadaDTO;
import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPessoa;

public abstract class PesPessoaEstabBaseService extends PesPessoaBaseService {


    protected void inserirCadUnicoPessoaJuridica(
            Long idPessoa,
            PesEmpresaVinculadaDTO estabelecimento) {

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
            :observacao,
            :email,
            :banco,
            :pessoasCdUnico
        )
        """)
                .setParameter("id", cadUnicoPessoaId)
                .setParameter(
                        "cdOrigem",
                        estabelecimento.pessoa()
                )
                .setParameter(
                        "tipoPessoa",
                        estabelecimento.tipoPessoa()
                )
                .setParameter(
                        "nome",
                        estabelecimento.nome() == null
                                ? null
                                : estabelecimento.nome().toUpperCase()
                )
                .setParameter(
                        "fisicaJuridica",
                        "J"
                )
                .setParameter(
                        "dataCadastro",
                        estabelecimento.dataCadastro()
                )
                .setParameter(
                        "cpfCnpj",
                        estabelecimento.cnpj()
                )
                .setParameter(
                        "observacao",
                        estabelecimento.observacao()
                )
                .setParameter(
                        "email",
                        estabelecimento.email()
                )
                .setParameter(
                        "banco",
                        "PESSOAS"
                )
                .setParameter(
                        "pessoasCdUnico",
                        idPessoa
                )
                .executeUpdate();
    }
    protected Long inserirDadosPj(
            Long pessoaCdUnicoId,
            PesEmpresaVinculadaDTO estabelecimento) {

        Long dadosPjId = getNextVal("SEQ_DADOS_PJ");
        String cnpj = normalizarCnpj(estabelecimento.cnpj());

        manager.createNativeQuery("""
        insert into DBO_CCM_PESSOAS.DADOS_PJ
        (
            ID,
            CNPJ,
            NOME_FANTASIA,
            OBJETO_SOCIAL,
            MICRO_EMPRESA,
            CONJUGE,
            TIPO_EMPRESA,
            NOME,
            ESTABELECIMENTO,
            PESSOA_ID
        )
        values
        (
            :id,
            :cnpj,
            :nomeFantasia,
            :objetoSocial,
            :microEmpresa,
            :conjuge,
            :tipoEmpresa,
            :nome,
            :estabelecimento,
            :pessoaId
        )
        """)
                .setParameter("id", dadosPjId)
                .setParameter("cnpj", cnpj)
                .setParameter(
                        "nomeFantasia",
                        estabelecimento.fantasia()
                )
                .setParameter(
                        "objetoSocial",
                        estabelecimento.objetoSocial()
                )
                .setParameter(
                        "microEmpresa",
                        estabelecimento.microempresa()
                )
                .setParameter("conjuge", null)
                .setParameter("tipoEmpresa", null)
                .setParameter(
                        "nome",
                        estabelecimento.nome()
                )
                .setParameter(
                        "estabelecimento",
                        estabelecimento.estabelecimento()
                )
                .setParameter(
                        "pessoaId",
                        pessoaCdUnicoId
                )
                .executeUpdate();

        return dadosPjId;
    }
    protected void inserirEnderecoEstabelecimento(
            Long cadUnicoPessoaId,
            PesEmpresaVinculadaDTO estabelecimento,
            Long dadosPjId) {

        PesPessoa pessoaEndereco = new PesPessoa();

        pessoaEndereco.setFisicaJuridica("J");
        pessoaEndereco.setCidade(estabelecimento.cidade());
        pessoaEndereco.setDistrito(estabelecimento.distrito());
        pessoaEndereco.setBairro(estabelecimento.bairro());
        pessoaEndereco.setLogradouro(estabelecimento.logradouro());

        pessoaEndereco.setNumero(
                estabelecimento.numero() == null
                        ? null
                        : estabelecimento.numero().longValue()
        );

        pessoaEndereco.setComplemento(
                estabelecimento.complemento()
        );

        pessoaEndereco.setCep(
                estabelecimento.cep()
        );

        inserirEnderecoPrincipal(
                cadUnicoPessoaId,
                pessoaEndereco,
                dadosPjId
        );
    }

    protected void inserirContatosEstabelecimento(
            Long idPessoa,
            PesEmpresaVinculadaDTO estabelecimento,
            Long dadosPjId) {

        if (estabelecimento.telefone() != null
                && !estabelecimento.telefone().isBlank()) {

            inserirContato(
                    idPessoa,
                    0L,
                    estabelecimento.telefone().trim(),
                    dadosPjId
            );
        }

        if (estabelecimento.celular() != null
                && !estabelecimento.celular().isBlank()) {

            inserirContato(
                    idPessoa,
                    1L,
                    estabelecimento.celular().trim(),
                    dadosPjId
            );
        }

        if (estabelecimento.email() != null
                && !estabelecimento.email().isBlank()) {

            inserirContato(
                    idPessoa,
                    3L,
                    estabelecimento.email().trim(),
                    dadosPjId
            );
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
    protected void inserirDocumentoEstabelecimento(
            Long idPessoa,
            PesEmpresaVinculadaDTO estabelecimento,
            Long dadosPjId) {

        String inscricaoEstadual =
                estabelecimento.inscricaoEstadual();

        if (inscricaoEstadual == null
                || inscricaoEstadual.isBlank()) {
            return;
        }

        inserirDocumentoSimples(
                idPessoa,
                2L,
                inscricaoEstadual.trim(),
                dadosPjId
        );
    }
    protected Long getNextVal(String sequence) {
        return ((Number) manager
                .createNativeQuery("select " + sequence + ".nextval from dual")
                .getSingleResult()).longValue();
    }
}