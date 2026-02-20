package br.com.cc.pessoas.dto.cep;

import br.com.cc.pessoas.entity.Cep;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CepDTO {

    private Long id;
    private String cep;
    private Integer numeroIni;
    private Integer numeroFim;
    private String identificacao;

    private Long logradouroId;
    private String nomeLogradouro;

    private Long bairroId;
    private String nomeBairro;

    private Long distritoId;
    private String nomeDistrito;

    private Long cidadeId;
    private String nomeCidade;

    public static CepDTO fromCep(Cep cep) {
        return new CepDTO(
                cep.getId(),
                cep.getCep(),
                cep.getNumeroIni(),
                cep.getNumeroFim(),
                cep.getIdentificacao(),

                cep.getLogradouro().getId(),
                cep.getLogradouro().getNome(),

                cep.getBairro().getId(),
                cep.getBairro().getNome(),

                cep.getBairro().getDistrito().getId(),
                cep.getBairro().getDistrito().getNome(),

                cep.getBairro().getDistrito().getCidade().getId(),
                cep.getBairro().getDistrito().getCidade().getNome()
        );
    }
}
