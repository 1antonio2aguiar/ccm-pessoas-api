package br.com.cc.pessoas.unificacao.pesEntity;

import java.io.Serializable;
import java.util.Objects;

public class PesBairroId implements Serializable {

    private Long cidade;
    private Long distrito;
    private Long bairro;

    public PesBairroId() {
    }

    public PesBairroId(Long cidade, Long distrito, Long bairro) {
        this.cidade = cidade;
        this.distrito = distrito;
        this.bairro = bairro;
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

    public Long getBairro() {
        return bairro;
    }

    public void setBairro(Long bairro) {
        this.bairro = bairro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PesBairroId that)) return false;
        return Objects.equals(cidade, that.cidade)
                && Objects.equals(distrito, that.distrito)
                && Objects.equals(bairro, that.bairro);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cidade, distrito, bairro);
    }
}