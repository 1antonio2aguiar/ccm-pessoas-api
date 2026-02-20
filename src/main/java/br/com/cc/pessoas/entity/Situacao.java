package br.com.cc.pessoas.entity;

import br.com.cc.pessoas.entity.base.BaseDescricao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SITUACOES", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Situacao extends BaseDescricao {

    @Id
    @GeneratedValue(generator = "SEQ_SITUACOES")
    @SequenceGenerator(
            name = "SEQ_SITUACOES",
            sequenceName = "SEQ_SITUACOES",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;
}
