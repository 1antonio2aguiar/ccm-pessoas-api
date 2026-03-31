package br.com.cc.pessoas.unificacao.pesEntity;

import java.io.Serializable;
import java.util.Objects;

public class PesCepId implements Serializable {

    private Long cidade;
    private Long distrito;
    private Long logradouro;
    private Long numero_ini;

    public PesCepId() {}

    public PesCepId(Long cidade, Long distrito, Long logradouro, Long numero_ini) {
        this.cidade = cidade;
        this.distrito = distrito;
        this.logradouro = logradouro;
        this.numero_ini = numero_ini;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PesCepId that)) return false;
        return Objects.equals(cidade, that.cidade)
                && Objects.equals(distrito, that.distrito)
                && Objects.equals(logradouro, that.logradouro)
                && Objects.equals(numero_ini, that.numero_ini);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cidade, distrito, logradouro, numero_ini);
    }
}