package br.com.cc.pessoas.unificacao.pessoa.pesService;

import br.com.cc.pessoas.unificacao.pessoa.pesDto.PesEmpresaVinculadaDTO;
import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPessoa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProcessarEmpresasVinculadas extends PesPessoaEstabBaseService {

    @PersistenceContext
    private EntityManager manager;

    public void processar(
            PesPessoa socioOrigem,
            Long idPessoa) {

        if (!possuiEstabelecimentos(
                socioOrigem.getPessoa())) {
            return;
        }

        List<PesEmpresaVinculadaDTO> estabelecimentos =
                buscarEstabelecimentos(
                        socioOrigem.getPessoa()
                );

        if (estabelecimentos.isEmpty()) {
            return;
        }

        for (PesEmpresaVinculadaDTO estabelecimento : estabelecimentos) {

            Long estabelecimentoOrigem = estabelecimento.estabelecimento();
            Long pessoaJuridicaOrigem  = estabelecimento.pessoa();
            Long cnpj = estabelecimento.cnpj();

            if (cnpj == null) {
                throw new RuntimeException(
                        "CNPJ não informado para a pessoa jurídica "
                                + pessoaJuridicaOrigem + "."
                );
            }

            Long idPessoaJuridicaDestino =
                buscarPessoaPorCnpj(
                        cnpj,
                        idPessoa
                );

            if (idPessoaJuridicaDestino == null) {
                idPessoaJuridicaDestino = criarPessoaJuridica(
                        estabelecimento,
                        idPessoa
                );
            }
        }


        // próximo passo
    }
    protected Long criarPessoaJuridica(
            PesEmpresaVinculadaDTO estabelecimento,
            Long idPessoa) {


        PesPessoa empresa = new PesPessoa();

        empresa.setNome(
                estabelecimento.nome()
        );

        empresa.setFisicaJuridica("J");

        empresa.setDataCadastro(
                estabelecimento.dataCadastro() == null
                        ? null
                        : estabelecimento.dataCadastro().atStartOfDay()
        );

        empresa.setObservacao(
                estabelecimento.observacao()
        );

        /*  CHAMA METODO QUE INSERE EM CAD_UNICO_PESSOA*/
        inserirCadUnicoPessoaJuridica(
                idPessoa,
                estabelecimento
        );

        /*  CHAMA METODO QUE INSERE EM DADOS_PJ*/
        Long dadosPjId = inserirDadosPj(
                idPessoa,
                estabelecimento
        );

        /*  CHAMA METODO QUE INSERE ENDEREÇOS DO ESTABELECIMENTO*/
        inserirEnderecoEstabelecimento(
                idPessoa,
                estabelecimento,
                dadosPjId
        );

        /*  CHAMA METODO QUE INSERE CONTATOS DO ESTABELECIMENTO*/
        inserirContatosEstabelecimento(
                idPessoa,
                estabelecimento,
                dadosPjId
        );

        inserirDocumentoEstabelecimento(
                idPessoa,
                estabelecimento,
                dadosPjId
        );

        return idPessoa;
    }

    @SuppressWarnings("unchecked")
    protected List<PesEmpresaVinculadaDTO> buscarEstabelecimentos(
            Long pessoaOrigem) {

        List<Object[]> resultados =
                manager.createNativeQuery("""
                select
                    soc.estabelecimento,
                    est.pessoa,
                    pes.nome,
                    pes.cgc_cpf,
                    pes.tipo_pessoa,
                    pes.cidade,
                    pes.distrito,
                    pes.bairro,
                    pes.logradouro,
                    pes.numero,
                    pes.complemento,
                    pes.cep,
                    pes.tipo_documento,
                    pes.numero_docto,
                    pes.telefone,
                    pes.celular,
                    pes.e_mail,
                    pes.inscricao_estadual,
                    pes.fantasia,
                    pes.objeto_social,
                    pes.microempresa,
                    pes.data_cadastro,
                    pes.observacao 
                from dbo_tributos.socios soc
                join dbo_tributos.estabelecimentos est
                  on est.estabelecimento = soc.estabelecimento
                join dbo_tributos.pessoas pes
                  on pes.pessoa = est.pessoa
                where soc.pessoa = :pessoa
            """)
                        .setParameter("pessoa", pessoaOrigem)
                        .getResultList();

        return resultados.stream()
                .map(resultado ->
                        new PesEmpresaVinculadaDTO(
                                toLong(resultado[0]),
                                toLong(resultado[1]),
                                toString(resultado[2]),
                                toLong(resultado[3]),
                                toLong(resultado[4]),
                                toLong(resultado[5]),
                                toLong(resultado[6]),
                                toLong(resultado[7]),
                                toLong(resultado[8]),
                                toInteger(resultado[9]),
                                toString(resultado[10]),
                                toLong(resultado[11]),
                                toLong(resultado[12]),
                                toString(resultado[13]),
                                toString(resultado[14]),
                                toString(resultado[15]),
                                toString(resultado[16]),
                                toString(resultado[17]),
                                toString(resultado[18]),
                                toString(resultado[19]),
                                toString(resultado[20]),
                                toLocalDate(resultado[21]),
                                toString(resultado[22])
                        )
                )
                .toList();
    }
    private Long toLong(Object valor) {
        return valor == null
                ? null
                : ((Number) valor).longValue();
    }

    private Integer toInteger(Object valor) {
        return valor == null
                ? null
                : ((Number) valor).intValue();
    }

    private String toString(Object valor) {
        return valor == null
                ? null
                : valor.toString();
    }
    private LocalDate toLocalDate(Object valor) {
        if (valor == null) {
            return null;
        }

        if (valor instanceof LocalDate localDate) {
            return localDate;
        }

        if (valor instanceof java.sql.Date dataSql) {
            return dataSql.toLocalDate();
        }

        if (valor instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }

        throw new IllegalArgumentException(
                "Não foi possível converter o valor para LocalDate: "
                        + valor
        );
    }
    protected Long buscarPessoaPorCnpj(
            Long cnpj,
            Long idPessoa) {

        List<?> resultado =
                manager.createNativeQuery("""
                select cup.id
                  from dbo_ccm_pessoas.cad_unico_pessoa cup
                 where cup.cpf_cnpj = :cnpj
                   and cup.pessoas_cd_unico = :idPessoa
                   and cup.fisica_juridica = 'J'
                   and cup.banco = 'PESSOAS'
            """)
                        .setParameter(
                                "cnpj",
                                cnpj
                        )
                        .setParameter(
                                "idPessoa",
                                idPessoa
                        )
                        .getResultList();

        if (resultado.isEmpty()) {
            return null;
        }

        return ((Number) resultado.get(0))
                .longValue();
    }

    public boolean possuiEstabelecimentos(Long pessoaOrigem) {

        String sql = """
            select 1
              from dbo_tributos.socios
             where pessoa = :pessoa
               and rownum = 1
            """;

        return !manager.createNativeQuery(sql)
                .setParameter("pessoa", pessoaOrigem)
                .getResultList()
                .isEmpty();
    }

    protected Long getNextVal(String sequence) {
        return ((Number) manager
                .createNativeQuery("select " + sequence + ".nextval from dual")
                .getSingleResult()).longValue();
    }
}