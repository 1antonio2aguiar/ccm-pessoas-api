package br.com.cc.pessoas.dto.contato;

import br.com.cc.pessoas.entity.Cidade;
import br.com.cc.pessoas.entity.Contato;

public record ContatoDTO(
        Long id,
        Long pessoaId,
        String pessoaNome,
        Integer tipoContato, // codigo do Enum
        String tipoContatoDescricao, // Descrição do Enum
        String contato,
        String principal,
        String complemento

) {

    public static ContatoDTO fromContato(Contato contato) {
        if (contato == null) return null;

        return new ContatoDTO(
                contato.getId(),
                contato.getPessoa().getId(),
                contato.getPessoa().getNome(),
                contato.getTipoContato() != null ? contato.getTipoContato().getCodigo() : null,
                contato.getTipoContato() != null ? contato.getTipoContato().getDescricao() : null,
                contato.getContato(),
                contato.getPrincipal(),
                contato.getComplemento()
        );
    }
}
