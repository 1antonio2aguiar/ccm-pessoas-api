package br.com.cc.pessoas.dto.cidade;

import br.com.cc.pessoas.entity.Cidade;

public record CidadeDTO(
        Long id,
        Long estadoId,
        String estadoUf,
        String estadoNome,
        String nome,
        String sigla,
        Long codigoSicom,
        Long codigoIbge,
        Long codigoInep
) {

    public static CidadeDTO fromCidade(Cidade cidade) {
        if (cidade == null) return null;

        return new CidadeDTO(
                cidade.getId(),
                cidade.getEstado().getId(),
                cidade.getEstado().getUf(),
                cidade.getEstado().getNome(),
                cidade.getNome(),
                cidade.getSigla(),
                cidade.getCodigoSicom(),
                cidade.getCodigoIbge(),
                cidade.getCodigoInep()
        );
    }
}
