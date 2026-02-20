package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DISTRITOS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Distrito {

    @Id
    @GeneratedValue(generator = "SEQ_DISTRITOS")
    @SequenceGenerator(
            name = "SEQ_DISTRITOS",
            sequenceName = "SEQ_DISTRITOS",
            allocationSize = 1
    )
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CIDADE_ID", nullable = false)
    private Cidade cidade;

    @Column(name = "NOME", length = 255, nullable = false)
    private String nome;

    @Column(name = "CODIGO_INEP")
    private Long codigoInep;
}
