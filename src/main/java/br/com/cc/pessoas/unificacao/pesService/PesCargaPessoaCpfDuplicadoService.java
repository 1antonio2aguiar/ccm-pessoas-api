package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.entity.enuns.TipoDocumento;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PesCargaPessoaCpfDuplicadoService extends PesCargaPessoaDuplicadoBaseService {

    @Override
    protected String getFisicaJuridica() {
        return "F";
    }

    @Override
    protected Integer getTipoEndereco() {
        return 0;
    }

    @Override
    protected String normalizarCpfCnpj(Long cgcCpf) {
        return normalizarCpf(cgcCpf);
    }

    @Override
    protected void insertDadosEspecificos(Long idPessoa, List<PesPessoa> grupo, DadosGrupo dadosGrupo) {
        insertDadosPfPrincipal(
                idPessoa,
                dadosGrupo.cpfCnpjPrincipal,
                dadosGrupo.nomeSocial,
                dadosGrupo.sexo,
                dadosGrupo.estadoCivil,
                dadosGrupo.cidadeNascimentoCcm,
                dadosGrupo.mae,
                dadosGrupo.pai,
                dadosGrupo.dataNascimento
        );
    }

    @Override
    protected void insertDocumentosEspecificos(Long idPessoa, List<PesPessoa> grupo) {
        insertDocumentosPf(idPessoa, grupo);
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

    private void insertDocumentosPf(Long idPessoa, List<PesPessoa> grupo) {
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
}
