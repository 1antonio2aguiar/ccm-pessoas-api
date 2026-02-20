package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TIPOS_LOGRADOUROS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoLogradouro {

    @Id
    @GeneratedValue(generator = "SEQ_TIPOS_LOGRADOUROS")
    @SequenceGenerator(
            name = "SEQ_TIPOS_LOGRADOUROS",
            sequenceName = "SEQ_TIPOS_LOGRADOUROS",
            allocationSize = 1
    )
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "DESCRICAO", length = 255, nullable = false)
    private String descricao;

    @Column(name = "SIGLA", length = 15, nullable = false)
    private String sigla;
}
