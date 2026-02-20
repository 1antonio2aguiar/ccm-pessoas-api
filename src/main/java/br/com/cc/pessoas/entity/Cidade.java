package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CIDADES", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cidade {

    @Id
    @GeneratedValue(generator = "SEQ_CIDADES")
    @SequenceGenerator(
            name = "SEQ_CIDADES",
            sequenceName = "SEQ_CIDADES",
            allocationSize = 1
    )
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ESTADO_ID", nullable = false)
    private Estado estado;

    @Column(name = "NOME", length = 255, nullable = false)
    private String nome;

    @Column(name = "SIGLA", length = 5)
    private String sigla;

    @Column(name = "CODIGO_SICOM")
    private Long codigoSicom;

    @Column(name = "CODIGO_IBGE")
    private Long codigoIbge;

    @Column(name = "CODIGO_INEP")
    private Long codigoInep;
}
