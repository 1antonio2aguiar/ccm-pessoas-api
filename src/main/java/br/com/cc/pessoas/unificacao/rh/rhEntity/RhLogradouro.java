package br.com.cc.pessoas.unificacao.rh.rhEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "RH_LOGRADOUROS", schema = "DBO_CCM_PESSOAS")
@IdClass(RhLogradouroId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RhLogradouro {

    @Id
    @Column(name = "CIDADE")
    private Long cidade;

    @Id
    @Column(name = "DISTRITO")
    private Long distrito;

    @Id
    @Column(name = "LOGRADOURO")
    private Long logradouro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TIPO_LOGRADOURO", nullable = false)
    private RhTipoLogradouro tipoLogradouro;

    @Column(name = "NOME")
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false)
    })
    private RhDistrito rhDistrito;
}