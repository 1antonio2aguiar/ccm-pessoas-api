package br.com.cc.pessoas.entity;

import br.com.cc.pessoas.entity.base.BaseDescricao;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TITULOS_PATENTES", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TituloPatente extends BaseDescricao {

    @Id
    @GeneratedValue(generator = "SEQ_TITULOS_PATENTES")
    @SequenceGenerator(
            name = "SEQ_TITULOS_PATENTES",
            sequenceName = "SEQ_TITULOS_PATENTES",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;
}
