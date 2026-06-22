package br.com.cc.pessoas.unificacao.saneamento.saneEntity;

import java.io.Serializable;
import java.util.Objects;

public class SaneDistritoId implements Serializable {

    private Long cidade;
    private Long distrito;

    public SaneDistritoId() {}

    public SaneDistritoId(Long cidade, Long distrito) {
        this.cidade = cidade;
        this.distrito = distrito;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaneDistritoId that)) return false;
        return Objects.equals(cidade, that.cidade)
                && Objects.equals(distrito, that.distrito);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cidade, distrito);
    }
}