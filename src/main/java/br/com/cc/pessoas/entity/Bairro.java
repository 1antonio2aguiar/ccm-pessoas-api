package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "BAIRROS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bairro {

    @Id
    @GeneratedValue(generator = "SEQ_BAIRROS")
    @SequenceGenerator(
            name = "SEQ_BAIRROS",
            sequenceName = "SEQ_BAIRROS",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DISTRITO_ID", nullable = false)
    private Distrito distrito;

    @Column(name = "NOME", nullable = false, length = 255)
    private String nome;

    @Column(name = "NOME_ABREVIADO", length = 100)
    private String nomeAbreviado;
}
