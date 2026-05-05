package br.com.cc.pessoas.unificacao.rh.rhEntity;

import java.io.Serializable;
import java.util.Objects;

public class RhDistritoId implements Serializable {

    private Long cidade;
    private Long distrito;

    public RhDistritoId() {}

    public RhDistritoId(Long cidade, Long distrito) {
        this.cidade = cidade;
        this.distrito = distrito;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RhDistritoId that)) return false;
        return Objects.equals(cidade, that.cidade)
                && Objects.equals(distrito, that.distrito);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cidade, distrito);
    }
}