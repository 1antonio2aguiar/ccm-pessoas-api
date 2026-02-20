package br.com.cc.pessoas.dto.pais;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaisUpdateDTO(

        @NotBlank
        @Size(max = 255)
        String nome,

        @Size(max = 15)
        String sigla,

        Long codigoInep,

        @Size(max = 50)
        String nacionalidade
) {

        public String getNome(){
                if (nome != null) {
                        return nome.toUpperCase();
                } else {
                        return "";
                }
        }

        public String getSigla(){
                if (sigla != null) {
                        return sigla.toUpperCase();
                } else {
                        return "";
                }
        }

        public String getNacionalidade(){
                if (nacionalidade != null) {
                        return nacionalidade.toUpperCase();
                } else {
                        return "";
                }
        }

}
