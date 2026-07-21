package br.com.cc.pessoas.dto.endereco;

import br.com.cc.pessoas.entity.DadosPessoaJuridica;
import br.com.cc.pessoas.entity.Endereco;
import jakarta.persistence.EntityNotFoundException;

public record EnderecoDTO(
        Long id,

        Long pessoaId,
        String pessoaNome,

        Long dadosPessoaJuridicaId,
        String estabelecimentoNome,
        String estabelecimentoCnpj,

        Integer tipoEndereco,
        String principal,

        Long tipoLogradouroId,
        String tipoLogradouro,

        Long logradouroId,
        String logradouroNome,

        Long numero,
        String complemento,

        Long bairroId,
        String bairroNome,

        String distritoNome,
        String cidadeNome,
        String estadoUf,

        Long cepId,
        String cep
) {

    public static EnderecoDTO fromEndereco(Endereco endereco) {

        if (endereco == null) {
            return null;
        }

        Long cepId = null;
        String cep = null;

        try {
            if (endereco.getCep() != null) {
                cepId = endereco.getCep().getId();
                cep = endereco.getCep().getCep();
            }
        } catch (EntityNotFoundException e) {
            cepId = null;
            cep = null;
        }

        DadosPessoaJuridica estabelecimento =
                endereco.getDadosPessoaJuridica();

        return new EnderecoDTO(
                endereco.getId(),

                endereco.getPessoa() != null
                        ? endereco.getPessoa().getId()
                        : null,

                endereco.getPessoa() != null
                        ? endereco.getPessoa().getNome()
                        : null,

                estabelecimento != null
                        ? estabelecimento.getId()
                        : null,

                estabelecimento != null
                        ? estabelecimento.getNome()
                        : null,

                estabelecimento != null
                        ? estabelecimento.getCnpj()
                        : null,

                endereco.getTipoEndereco() != null
                        ? endereco.getTipoEndereco().getCodigo()
                        : null,

                endereco.getPrincipal(),

                endereco.getLogradouro() != null
                        && endereco.getLogradouro().getTipoLogradouro() != null
                        ? endereco.getLogradouro().getTipoLogradouro().getId()
                        : null,

                endereco.getLogradouro() != null
                        && endereco.getLogradouro().getTipoLogradouro() != null
                        ? endereco.getLogradouro().getTipoLogradouro().getSigla()
                        : null,

                endereco.getLogradouro() != null
                        ? endereco.getLogradouro().getId()
                        : null,

                endereco.getLogradouro() != null
                        ? endereco.getLogradouro().getNome()
                        : null,

                endereco.getNumero(),
                endereco.getComplemento(),

                endereco.getBairro() != null
                        ? endereco.getBairro().getId()
                        : null,

                endereco.getBairro() != null
                        ? endereco.getBairro().getNome()
                        : null,

                endereco.getLogradouro() != null
                        && endereco.getLogradouro().getDistrito() != null
                        ? endereco.getLogradouro().getDistrito().getNome()
                        : null,

                endereco.getLogradouro() != null
                        && endereco.getLogradouro().getDistrito() != null
                        && endereco.getLogradouro().getDistrito().getCidade() != null
                        ? endereco.getLogradouro()
                        .getDistrito()
                        .getCidade()
                        .getNome()
                        : null,

                endereco.getLogradouro() != null
                        && endereco.getLogradouro().getDistrito() != null
                        && endereco.getLogradouro().getDistrito().getCidade() != null
                        && endereco.getLogradouro()
                        .getDistrito()
                        .getCidade()
                        .getEstado() != null
                        ? endereco.getLogradouro()
                        .getDistrito()
                        .getCidade()
                        .getEstado()
                        .getUf()
                        : null,

                cepId,
                cep
        );
    }
}