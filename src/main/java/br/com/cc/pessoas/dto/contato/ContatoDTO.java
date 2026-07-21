package br.com.cc.pessoas.dto.contato;

import br.com.cc.pessoas.entity.Contato;
import br.com.cc.pessoas.entity.DadosPessoaJuridica;

public record ContatoDTO(
        Long id,

        Long pessoaId,
        String pessoaNome,

        Long dadosPessoaJuridicaId,
        String estabelecimentoNome,
        String estabelecimentoCnpj,

        Integer tipoContato,
        String tipoContatoDescricao,

        String contato,
        String principal,
        String complemento

) {

    public static ContatoDTO fromContato(Contato contato) {

        if (contato == null) {
            return null;
        }

        DadosPessoaJuridica estabelecimento =
                contato.getDadosPessoaJuridica();

        return new ContatoDTO(
                contato.getId(),

                contato.getPessoa() != null
                        ? contato.getPessoa().getId()
                        : null,

                contato.getPessoa() != null
                        ? contato.getPessoa().getNome()
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

                contato.getTipoContato() != null
                        ? contato.getTipoContato().getCodigo()
                        : null,

                contato.getTipoContato() != null
                        ? contato.getTipoContato().getDescricao()
                        : null,

                contato.getContato(),
                contato.getPrincipal(),
                contato.getComplemento()
        );
    }
}