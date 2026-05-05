package br.com.cc.pessoas.unificacao.rh.rhEntity;

import java.io.Serializable;
import java.util.Objects;

public class RhLogradouroId implements Serializable {

    private Long cidade;
    private Long distrito;
    private Long logradouro;

    public RhLogradouroId() {
    }

    public RhLogradouroId(Long cidade, Long distrito, Long logradouro) {
        this.cidade = cidade;
        this.distrito = distrito;
        this.logradouro = logradouro;
    }

    public Long getCidade() {
        return cidade;
    }

    public void setCidade(Long cidade) {
        this.cidade = cidade;
    }

    public Long getDistrito() {
        return distrito;
    }

    public void setDistrito(Long distrito) {
        this.distrito = distrito;
    }

    public Long getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(Long logradouro) {
        this.logradouro = logradouro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RhLogradouroId that)) return false;
        return Objects.equals(cidade, that.cidade)
                && Objects.equals(distrito, that.distrito)
                && Objects.equals(logradouro, that.logradouro);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cidade, distrito, logradouro);
    }
}