package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ESTADOS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estado {

    @Id
    @GeneratedValue(generator = "SEQ_ESTADOS")
    @SequenceGenerator(
            name = "SEQ_ESTADOS",
            sequenceName = "SEQ_ESTADOS",
            allocationSize = 1
    )
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PAIS_ID", nullable = false)
    private Pais pais;

    @Column(name = "NOME", length = 255, nullable = false)
    private String nome;

    @Column(name = "SIGLA", length = 5, nullable = false)
    private String sigla;

    @Column(name = "CODIGO_INEP")
    private Long codigoInep;
}
