package br.com.cc.pessoas.unificacao.pesEntity;

import br.com.cc.pessoas.entity.TipoLogradouro;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PES_LOGRADOUROS", schema = "DBO_CCM_PESSOAS")
@IdClass(PesLogradouroId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PesLogradouro {

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
    private PesTipoLogradouro tipoLogradouro;

    @Column(name = "NOME")
    private String nome;

    @Column(name = "NOME_LEGAL")
    private String nomeLegal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false)
    })
    private PesDistrito pesDistrito;
}