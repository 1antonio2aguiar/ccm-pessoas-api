package br.com.cc.pessoas.dto.pais;

import br.com.cc.pessoas.entity.Pais;

import java.util.Optional;

public record PaisDTO(
        Long id,
        String nome,
        String sigla,
        Long codigoInep,
        String nacionalidade
) {


    /**
     * Factory method a partir da entidade Pais
     */
    public static PaisDTO fromPais(Pais pais) {
        if (pais == null) {
            return null;
        }

        return new PaisDTO(
                pais.getId(),
                pais.getNome(),
                pais.getSigla(),
                pais.getCodigoInep(),
                pais.getNacionalidade()
        );
    }

    /**
     * Factory method para Optional<Pais>
     * (muito útil em services e repositories)
     */
    public static PaisDTO fromOptionalPais(Optional<Pais> pais) {
        return pais.map(PaisDTO::fromPais).orElse(null);
    }
}

