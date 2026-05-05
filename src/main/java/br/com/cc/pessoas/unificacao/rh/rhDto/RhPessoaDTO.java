package br.com.cc.pessoas.unificacao.rh.rhDto;

import br.com.cc.pessoas.unificacao.rh.rhEntity.*;

import java.time.LocalDateTime;

public record RhPessoaDTO(
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
        Long dddTelefone,
        Long telefone,
        Long dddRecado,
        Long recado,
        Long dddCelular,
        Long celular,
        Long whatsapp,
        Long fax,
        String email,
        String paginaWeb,
        Long pessoaMatriz,
        String inscricaoEstadual,
        String fantasia,
        Long profissao,
        String vip,
        String nomeConjuge,
        Long mesEnvioSicom,
        String nomeSocial,
        String instagram,
        String facebook,
        String statusCadastro
) {
    public static RhPessoaDTO fromEntity(RhPessoa entity) {
        RhCidade cidade = entity.getRhCidade();
        RhDistrito distrito = entity.getRhDistrito();
        RhBairro bairro = entity.getRhBairro();
        RhLogradouro logradouro = entity.getRhLogradouro();
        RhTipoPessoa rhTipoPessoa = entity.getRhTipoPessoa();
        RhTipoDocumento rhTipoDocumento = entity.getRhTipoDocumento();

        return new RhPessoaDTO(
                entity.getPessoa(),
                entity.getNome(),
                entity.getFisicaJuridica(),
                entity.getDataCadastro(),
                entity.getCgcCpf(),

                rhTipoPessoa != null ? Long.valueOf(rhTipoPessoa.getTipoPessoa()) : null,
                rhTipoPessoa != null ? rhTipoPessoa.getDescricao() : null,

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

                rhTipoDocumento != null ? Long.valueOf(rhTipoDocumento.getTipoDocumento()) : null,
                rhTipoDocumento != null ? rhTipoDocumento.getDescricao() : null,

                entity.getNumeroDocto(),
                entity.getOrgaoDocto(),
                entity.getEmissaoDocto(),

                entity.getTituloEleitoral(),
                entity.getZona(),
                entity.getSecao(),

                entity.getMae(),
                entity.getPai(),

                entity.getDddTelefone(),
                entity.getTelefone(),
                entity.getDddRecado(),
                entity.getRecado(),
                entity.getDddCelular(),
                entity.getCelular(),
                entity.getWhatsapp(),
                entity.getFax(),

                entity.getEmail(),
                entity.getPaginaWeb(),

                entity.getPessoaMatriz(),
                entity.getInsricaoEstadual(),
                entity.getFantasia(),
                entity.getProfissao(),
                entity.getVip(),
                entity.getConjuge(),
                entity.getMesEnvioSicom(),
                entity.getNomeSocial(),
                entity.getInstagram(),
                entity.getFacebook(),
                null
        );
    }
}