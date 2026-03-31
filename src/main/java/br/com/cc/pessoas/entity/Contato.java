package br.com.cc.pessoas.entity;

import br.com.cc.pessoas.entity.enuns.TipoContato;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CONTATOS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contato {

    @Id
    @GeneratedValue(generator = "SEQ_CONTATOS")
    @SequenceGenerator(
            name = "SEQ_CONTATOS",
            sequenceName = "SEQ_CONTATOS",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    private TipoContato tipoContato;
    private String contato;
    private String principal;
    private String complemento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

}
