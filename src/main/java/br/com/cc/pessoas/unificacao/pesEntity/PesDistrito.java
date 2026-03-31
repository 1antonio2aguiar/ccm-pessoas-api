package br.com.cc.pessoas.unificacao.pesEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PES_DISTRITOS", schema = "DBO_CCM_PESSOAS")
@IdClass(PesDistritoId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PesDistrito {

    @Id
    @Column(name = "DISTRITO")
    private Long distrito;

    @Id
    @Column(name = "CIDADE")
    private Long cidade;

    @Column(name = "NOME")
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CIDADE", insertable = false, updatable = false)
    private PesCidade pesCidade;
}