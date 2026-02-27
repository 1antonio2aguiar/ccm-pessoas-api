package br.com.cc.pessoas.dto.cep;

public record CepUpdateDTO(
        Long logradouroId,
        Long bairroId,
        Integer numeroIni,
        Integer numeroFim,
        String identificacao
) {
    public String getIdentificacao() {
        return identificacao != null ? identificacao.toUpperCase() : "";
    }
}
