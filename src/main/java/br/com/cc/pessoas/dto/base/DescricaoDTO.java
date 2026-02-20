package br.com.cc.pessoas.dto.base;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DescricaoDTO {

    private Long id;

    @NotBlank
    @Size(max = 255)
    private String descricao;

    public String getDescricao() {
        return descricao != null ? descricao.toUpperCase() : "";
    }
}
