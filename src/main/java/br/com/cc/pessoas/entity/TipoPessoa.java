package br.com.cc.pessoas.entity;

import br.com.cc.pessoas.entity.base.BaseDescricao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "TIPOS_PESSOAS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoPessoa extends BaseDescricao {

    @Id
    @GeneratedValue(generator = "SEQ_TIPOS_PESSOAS")
    @SequenceGenerator(
            name = "SEQ_TIPOS_PESSOAS",
            sequenceName = "SEQ_TIPOS_PESSOAS",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;
}
