package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LOGRADOUROS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Logradouro {

    @Id
    @GeneratedValue(generator = "SEQ_LOGRADOUROS")
    @SequenceGenerator(
            name = "SEQ_LOGRADOUROS",
            sequenceName = "SEQ_LOGRADOUROS",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DISTRITO_ID", nullable = false)
    private Distrito distrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TIPO_LOGRADOURO_ID", nullable = false)
    private TipoLogradouro tipoLogradouro;

    @Column(name = "NOME", nullable = false, length = 255)
    private String nome;

    @Column(name = "PREPOSICAO", length = 50)
    private String preposicao;

    @JoinColumn(name="TITULO_PATENTE")
    private String tituloPatente;

    @Column(name = "NOME_REDUZIDO", length = 100)
    private String nomeReduzido;

    @Column(name = "NOME_SIMPLIFICADO", length = 255)
    private String nomeSimplificado;

    @Column(name = "COMPLEMENTO", length = 255)
    private String complemento;
}
