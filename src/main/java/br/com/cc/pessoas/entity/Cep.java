package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CEPS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cep {

    @Id
    @GeneratedValue(generator = "SEQ_CEPS")
    @SequenceGenerator(
            name = "SEQ_CEPS",
            sequenceName = "SEQ_CEPS",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOGRADOURO_ID", nullable = false)
    private Logradouro logradouro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BAIRRO_ID", nullable = false)
    private Bairro bairro;

    @Column(name = "CEP", nullable = false, length = 20)
    private String cep;

    @Column(name = "NUMERO_INI")
    private Integer numeroIni;

    @Column(name = "NUMERO_FIM")
    private Integer numeroFim;

    @Column(name = "IDENTIFICACAO", length = 255)
    private String identificacao;
}
