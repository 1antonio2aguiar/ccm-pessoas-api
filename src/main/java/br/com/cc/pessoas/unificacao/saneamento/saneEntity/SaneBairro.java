package br.com.cc.pessoas.unificacao.saneamento.saneEntity;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhBairroId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SANE_BAIRROS", schema = "DBO_CCM_PESSOAS")
@IdClass(RhBairroId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaneBairro {

    @Id
    @Column(name = "CIDADE")
    private Long cidade;

    @Id
    @Column(name = "DISTRITO")
    private Long distrito;

    @Id
    @Column(name = "BAIRRO")
    private Long bairro;

    @Column(name = "NOME")
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false)
    })
    private SaneDistrito saneDistrito;
}