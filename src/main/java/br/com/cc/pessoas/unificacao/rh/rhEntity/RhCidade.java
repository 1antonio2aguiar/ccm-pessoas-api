package br.com.cc.pessoas.unificacao.rh.rhEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "RH_CIDADES", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RhCidade {

    @Id
    @Column(name = "CIDADE")
    private Long cidade;
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ESTADO", nullable = false)
    private RhEstado estado;
}
