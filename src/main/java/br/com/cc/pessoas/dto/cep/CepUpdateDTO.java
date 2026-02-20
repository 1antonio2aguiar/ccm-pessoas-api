package br.com.cc.pessoas.dto.cep;

public record CepUpdateDTO(
        String cep,
        Integer numeroIni,
        Integer numeroFim,
        String identificacao
) {

    public String getCep() {
        return cep != null ? cep.replaceAll("\\D", "") : "";
    }
}
