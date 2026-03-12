package br.com.cc.pessoas.dto.cep;

import br.com.cc.pessoas.entity.Cep;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private String logradouroNome;
    private String tipoLogradouro;

    private List<BairroInfoDTO> bairros;

    private Long distritoId;
    private String distritoNome;

    private Long cidadeId;
    private String cidadeNome;
    private String estadoUf;
    private String bairroNome;

    public static CepDTO fromCep(Cep cep) {

        List<BairroInfoDTO> bairros = cep.getBairro() != null
                ? Collections.singletonList(
                new BairroInfoDTO(
                        cep.getBairro().getId(),
                        cep.getBairro().getNome()
                )
        )
        : Collections.emptyList();

        return new CepDTO(
                cep.getId(),
                cep.getCep(),
                cep.getNumeroIni(),
                cep.getNumeroFim(),
                cep.getIdentificacao(),

                cep.getLogradouro().getId(),
                cep.getLogradouro().getNome(),
                cep.getLogradouro().getTipoLogradouro().getSigla(),

                bairros,

                cep.getLogradouro().getDistrito().getId(),
                cep.getLogradouro().getDistrito().getNome(),

                cep.getLogradouro().getDistrito().getCidade().getId(),
                cep.getLogradouro().getDistrito().getCidade().getNome(),
                cep.getLogradouro().getDistrito().getCidade().getEstado().getUf(),
                cep.getBairro().getNome()
        );
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class BairroInfoDTO {
        private Long id;
        private String nome;
    }
}