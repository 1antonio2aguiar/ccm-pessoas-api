package br.com.cc.pessoas.unificacao.rh.rhEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "RH_BAIRROS", schema = "DBO_CCM_PESSOAS")
@IdClass(RhBairro.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RhBairro {

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

    @Column(name = "CIDADE_MACRO_BAIRRO")
    private Long cidadeMacroBairro;

    @Column(name = "DISTRITO_MACRO_BAIRRO")
    private Long distritoMacroBairro;

    @Column(name = "MACRO_BAIRRO")
    private Long macroBairro;

    @Column(name = "ZONA_RURAL")
    private String zonaRural;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false)
    })
    private RhDistrito rhDistrito;
}