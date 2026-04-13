package br.com.cc.pessoas.unificacao.pesEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PESSOAS_DUPLICADAS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pessoasduplicadas {

    @Id
    @Column(name = "ID")
    private Long id;

    private String banco;

    private Long cpf;

    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CD_1", nullable = false)
    private PesPessoa pessoaCd1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CD_2", nullable = false)
    private PesPessoa pessoaCd2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CD_3")
    private PesPessoa pessoaCd3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CD_4")
    private PesPessoa pessoaCd4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CD_5")
    private PesPessoa pessoaCd5;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CD_6")
    private PesPessoa pessoaCd6;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CD_7")
    private PesPessoa pessoaCd7;

    @Transient
    public List<PesPessoa> getPessoasDuplicadasLista() {
        List<PesPessoa> lista = new ArrayList<>();

        if (pessoaCd1 != null) lista.add(pessoaCd1);
        if (pessoaCd2 != null) lista.add(pessoaCd2);
        if (pessoaCd3 != null) lista.add(pessoaCd3);
        if (pessoaCd4 != null) lista.add(pessoaCd4);
        if (pessoaCd5 != null) lista.add(pessoaCd5);
        if (pessoaCd6 != null) lista.add(pessoaCd6);
        if (pessoaCd7 != null) lista.add(pessoaCd7);

        return lista;
    }

    @Transient
    public List<Long> getCodigosPessoasDuplicadas() {
        List<Long> lista = new ArrayList<>();

        if (pessoaCd1 != null) lista.add(pessoaCd1.getPessoa());
        if (pessoaCd2 != null) lista.add(pessoaCd2.getPessoa());
        if (pessoaCd3 != null) lista.add(pessoaCd3.getPessoa());
        if (pessoaCd4 != null) lista.add(pessoaCd4.getPessoa());
        if (pessoaCd5 != null) lista.add(pessoaCd5.getPessoa());
        if (pessoaCd6 != null) lista.add(pessoaCd6.getPessoa());
        if (pessoaCd7 != null) lista.add(pessoaCd7.getPessoa());

        return lista;
    }

}
