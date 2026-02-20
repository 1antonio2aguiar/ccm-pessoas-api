package br.com.cc.pessoas.dto.bairro;

import br.com.cc.pessoas.entity.Bairro;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BairroDTO {

    private Long id;
    private String nome;
    private String nomeAbreviado;

    private Long distritoId;
    private Long cidadeId;
    private String nomeCidade;

    public static BairroDTO fromBairro(Bairro bairro) {
        return new BairroDTO(
                bairro.getId(),
                bairro.getNome(),
                bairro.getNomeAbreviado(),
                bairro.getDistrito().getId(),
                bairro.getDistrito().getCidade().getId(),
                bairro.getDistrito().getCidade().getNome()
        );
    }
}
