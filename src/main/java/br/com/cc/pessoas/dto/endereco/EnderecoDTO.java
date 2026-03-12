package br.com.cc.pessoas.dto.endereco;

import br.com.cc.pessoas.entity.Endereco;
import jakarta.persistence.EntityNotFoundException;

public record EnderecoDTO(
        Long id,
        String pessoaNome,
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

        return new EnderecoDTO(
                endereco.getId(),
                endereco.getPessoa().getNome(),
                endereco.getTipoEndereco() != null ? endereco.getTipoEndereco().getCodigo() : null,
                endereco.getPrincipal(),
                endereco.getLogradouro().getTipoLogradouro().getId(),
                endereco.getLogradouro().getTipoLogradouro().getSigla(),
                endereco.getLogradouro().getId(),
                endereco.getLogradouro().getNome(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro() != null ? endereco.getBairro().getId() : null,
                endereco.getBairro() != null ? endereco.getBairro().getNome() : null,
                endereco.getLogradouro().getDistrito().getNome(),
                endereco.getLogradouro().getDistrito().getCidade().getNome(),
                endereco.getLogradouro().getDistrito().getCidade().getEstado().getUf(),
                cepId,
                cep
        );
    }
}