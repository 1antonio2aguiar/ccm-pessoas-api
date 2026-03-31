package br.com.cc.pessoas.unificacao.pesDto;

import br.com.cc.pessoas.unificacao.pesEntity.PesBairro;
import br.com.cc.pessoas.unificacao.pesEntity.PesCidade;
import br.com.cc.pessoas.unificacao.pesEntity.PesDistrito;
import br.com.cc.pessoas.unificacao.pesEntity.PesLogradouro;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;

import java.time.LocalDateTime;

public record PesPessoaDTO(
        Long pessoa,
        String nome,
        String fisicaJuridica,
        Long cgcCpf,
        Long tipoPessoa,

        Long cidade,
        String cidadeNome,
        String uf,

        Long distrito,
        String distritoNome,

        Long bairro,
        String bairroNome,

        Long logradouro,
        String logradouroNome,

        Long numero,
        Long complemento,
        Long cep,

        Integer tipoDocumento,
        Long numeroDocto,
        Long orgaoDocto,
        LocalDateTime dataDocto,

        Long telefone,
        Long recado,
        Long celular,
        Long fax,
        String email,
        String paginaWeb,
        String vip,
        String sexo,
        String estadoCivil,
        LocalDateTime dataNascimento,
        String pai,
        String mae,
        String nomeSocial,
        Long zona,
        Long secao,
        Long tipoEmpresa,
        Long pessoaMatriz,
        String inscricaoEstadual,
        String fantasia,
        String objetoSocial,
        String microEmpresa,
        Long conjuge,
        Long profissao,
        Long cidadeNascimento,
        LocalDateTime dataCadastro,
        String observacao,
        Long usuario,
        Long usuarioAlteracao,
        Long mesEnvioSicom,
        Long anoEnvioSicom,
        String deficiente
) {
    public static PesPessoaDTO fromEntity(PesPessoa entity) {
        PesCidade cidade = entity.getPesCidade();
        PesDistrito distrito = entity.getPesDistrito();
        PesBairro bairro = entity.getPesBairro();
        PesLogradouro logradouro = entity.getPesLogradouro();

        return new PesPessoaDTO(
                entity.getPessoa(),
                entity.getNome(),
                entity.getFisicaJuridica(),
                entity.getCgcCpf(),
                entity.getTipoPessoa(),

                cidade != null ? cidade.getCidade() : null,
                cidade != null ? cidade.getNome() : null,
                cidade != null && cidade.getEstado() != null ? cidade.getEstado().getEstado() : null,

                distrito != null ? distrito.getDistrito() : null,
                distrito != null ? distrito.getNome() : null,

                bairro != null ? bairro.getBairro() : null,
                bairro != null ? bairro.getNome() : null,

                logradouro != null ? logradouro.getLogradouro() : null,
                logradouro != null ? logradouro.getNome() : null,

                entity.getNumero(),
                entity.getComplemento(),
                entity.getCep(),

                entity.getTipoDocumento() != null ? entity.getTipoDocumento().getTipoDocumento() : null,
                entity.getNumeroDocto(),
                entity.getOrgaoDocto(),
                entity.getDataDocto(),

                entity.getTelefone(),
                entity.getRecado(),
                entity.getCelular(),
                entity.getFax(),
                entity.getEmail(),
                entity.getPaginaWeb(),
                entity.getVip(),
                entity.getSexo(),
                entity.getEstadoCivil(),
                entity.getDataNascimento(),
                entity.getPai(),
                entity.getMae(),
                entity.getNomeSocial(),
                entity.getZona(),
                entity.getSecao(),
                entity.getTipoEmpresa(),
                entity.getPessoaMatriz(),
                entity.getInsricaoEstadual(),
                entity.getFantasia(),
                entity.getObjetoSocial(),
                entity.getMicroEmpresa(),
                entity.getConjuge(),
                entity.getProfissao(),
                entity.getCidadeNascimento(),
                entity.getDataCadastro(),
                entity.getObservacao(),
                entity.getUsuario(),
                entity.getUsuarioAlteracao(),
                entity.getMesEnvioSicom(),
                entity.getAnoEnvioSicom(),
                entity.getDeficiente()
        );
    }
}