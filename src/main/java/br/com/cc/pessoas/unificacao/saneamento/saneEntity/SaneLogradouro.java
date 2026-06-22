package br.com.cc.pessoas.unificacao.saneamento.saneEntity;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhLogradouroId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SANE_LOGRADOUROS", schema = "DBO_CCM_PESSOAS")
@IdClass(RhLogradouroId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaneLogradouro {

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
    private SaneTipoLogradouro tipoLogradouro;

    @Column(name = "NOME")
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false)
    })
    private SaneDistrito saneDistrito;
}