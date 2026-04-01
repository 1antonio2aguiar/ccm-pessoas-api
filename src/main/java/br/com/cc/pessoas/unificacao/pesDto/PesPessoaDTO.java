package br.com.cc.pessoas.unificacao.pesDto;

import br.com.cc.pessoas.entity.TipoPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.*;

import java.time.LocalDateTime;

public record PesPessoaDTO(
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
        Long fax,
        String email,
        String paginaWeb,

        Long pessoaMatriz,
        String inscricaoEstadual,
        String fantasia,
        Long profissao,
        String vip,
        Long usuario,
        String observacao,
        Long conjuge,
        LocalDateTime dtAlteracao,
        String usuarioAlteracao,
        String objetoSocial,
        String microEmpresa,
        Long mesEnvioSicom,
        Long anoEnvioSicom,
        Long tipoEmpresa,
        String nomeSocial,
        String deficiente
) {
    public static PesPessoaDTO fromEntity(PesPessoa entity) {
        PesCidade cidade = entity.getPesCidade();
        PesDistrito distrito = entity.getPesDistrito();
        PesBairro bairro = entity.getPesBairro();
        PesLogradouro logradouro = entity.getPesLogradouro();
        PesTipoPessoa pesTipoPessoa = entity.getPesTipoPessoa();
        PesTipoDocumento pesTipoDocumento = entity.getPesTipoDocumento();

        return new PesPessoaDTO(
                entity.getPessoa(),
                entity.getNome(),
                entity.getFisicaJuridica(),
                entity.getDataCadastro(),
                entity.getCgcCpf(),

                pesTipoPessoa != null ? Long.valueOf(pesTipoPessoa.getTipoPessoa()) : null,
                pesTipoPessoa != null ? pesTipoPessoa.getDescricao() : null,

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

                pesTipoDocumento != null ? Long.valueOf(pesTipoDocumento.getTipoDocumento()) : null,
                pesTipoDocumento != null ? pesTipoDocumento.getDescricao() : null,

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
                entity.getFax(),
                entity.getEmail(),
                entity.getPaginaWeb(),

                entity.getPessoaMatriz(),
                entity.getInsricaoEstadual(),
                entity.getFantasia(),
                entity.getProfissao(),
                entity.getVip(),
                entity.getUsuario(),
                entity.getObservacao(),
                entity.getConjuge(),
                entity.getDtAlteracao(),
                entity.getUsuarioAlteracao(),
                entity.getObjetoSocial(),
                entity.getMicroEmpresa(),
                entity.getMesEnvioSicom(),
                entity.getAnoEnvioSicom(),
                entity.getTipoEmpresa(),
                entity.getNomeSocial(),
                entity.getDeficiente()
        );
    }
}