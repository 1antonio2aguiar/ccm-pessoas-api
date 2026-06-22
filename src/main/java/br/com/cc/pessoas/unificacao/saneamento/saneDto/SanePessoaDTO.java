package br.com.cc.pessoas.unificacao.saneamento.saneDto;

import br.com.cc.pessoas.unificacao.saneamento.saneEntity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SanePessoaDTO(
        Long pessoa,
        String nome,
        String fisicaJuridica,
        LocalDateTime dataCadastro,
        Long cgcCpf,
        Long tipoPessoa,
        String tipoPessoaDescricao,
        Long cidade,
        String cidadeNome,
        String uf,
        Long distrito,
        String distritoNome,
        Long bairro,
        String bairroNome,
        Long logradouro,
        String logradouroNome,
        String tipoLogradouro,
        Long numero,
        String complemento,
        Long cep,
        LocalDateTime dataNascimento,
        String estadoCivil,
        String sexo,
        Long cidadeNascimento,
        String cidadeNascimentoNome,
        Long pais,
        Long tipoDocumento,
        String tipoDocumentoDescricao,
        String numeroDocto,
        String orgaoDocto,
        LocalDateTime emissaoDocto,
        Long tituloEleitoral,
        Long zona,
        Long secao,
        String mae,
        String pai,
        Long telefone,
        Long recado,
        Long celular,
        String email,
        String paginaWeb,
        Long pessoaMatriz,
        String inscricaoEstadual,
        String fantasia,
        Long profissao,
        String vip,
        String observacao,
        String aposentado,
        LocalDateTime inicioBeneficio,
        LocalDateTime fimBeneficio,
        BigDecimal rendaMensal,
        String statusCadastro

) {
    public static SanePessoaDTO fromEntity(SanePessoa entity) {
        SaneCidade cidade = entity.getSaneCidade();
        SaneDistrito distrito = entity.getSaneDistrito();
        SaneBairro bairro = entity.getSaneBairro();
        SaneLogradouro logradouro = entity.getSaneLogradouro();
        SaneTipoPessoa saneTipoPessoa = entity.getSaneTipoPessoa();
        SaneTipoDocumento saneTipoDocumento = entity.getSaneTipoDocumento();

        return new SanePessoaDTO(
                entity.getPessoa(),
                entity.getNome(),
                entity.getFisicaJuridica(),
                entity.getDataCadastro(),
                entity.getCgcCpf(),

                saneTipoPessoa != null ? Long.valueOf(saneTipoPessoa.getTipoPessoa()) : null,
                saneTipoPessoa != null ? saneTipoPessoa.getDescricao() : null,

                entity.getCidade(),
                cidade != null ? cidade.getNome() : null,
                cidade != null && cidade.getEstado() != null ? cidade.getEstado().getEstado() : null,

                entity.getDistrito(),
                distrito != null ? distrito.getNome() : null,

                entity.getBairro(),
                bairro != null ? bairro.getNome() : null,

                entity.getLogradouro(),
                logradouro != null ? logradouro.getNome() : null,

                logradouro != null && logradouro.getTipoLogradouro() != null
                        ? String.valueOf(logradouro.getTipoLogradouro().getTipoLogradouro())
                        : null,

                entity.getNumero(),
                entity.getComplemento(),
                entity.getCep(),

                entity.getDataNascimento(),
                entity.getEstadoCivil(),
                entity.getSexo(),

                entity.getCidadeNascimento(),
                null,

                entity.getPais(),

                saneTipoDocumento != null ? Long.valueOf(saneTipoDocumento.getTipoDocumento()) : null,
                saneTipoDocumento != null ? saneTipoDocumento.getDescricao() : null,

                entity.getNumeroDocto(),
                entity.getOrgaoDocto(),
                entity.getEmissaoDocto(),

                entity.getTituloEleitoral(),
                entity.getZona(),
                entity.getSecao(),

                entity.getMae(),
                entity.getPai(),

                entity.getTelefone(),
                entity.getRecado(),
                entity.getCelular(),

                entity.getEmail(),
                entity.getPaginaWeb(),

                entity.getPessoaMatriz(),
                entity.getInsricaoEstadual(),
                entity.getFantasia(),
                entity.getProfissao(),
                entity.getVip(),
                entity.getObservacao(),
                entity.getAposentado(),
                entity.getInicioBenceficio(),
                entity.getFimBenceficio(),
                entity.getRendaMensal(),
                null
        );
    }
}