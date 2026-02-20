package br.com.cc.pessoas.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseDescricao {

    @Column(name = "DESCRICAO", nullable = false, length = 255)
    protected String descricao;
}
