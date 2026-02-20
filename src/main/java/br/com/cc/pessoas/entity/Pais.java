package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PAISES", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pais {

    @Id
    @GeneratedValue(generator = "SEQ_PAISES")
    @SequenceGenerator(name = "SEQ_PAISES", sequenceName = "SEQ_PAISES", allocationSize = 1)
    @Column(name = "ID", unique = true, nullable = false)

    private Long id;

    @Column(name = "NOME", length = 255, nullable = false)
    private String nome;

    @Column(name = "SIGLA", length = 15)
    private String sigla;

    @Column(name = "CODIGO_INEP")
    private Long codigoInep;

    @Column(name = "NACIONALIDADE", length = 50)
    private String nacionalidade;
}