package br.com.cc.pessoas.unificacao.pesEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PES_CEPS", schema = "DBO_CCM_PESSOAS")
@IdClass(PesCepId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PesCep {

    @Id
    @Column(name = "CIDADE")
    private Long cidade;

    @Id
    @Column(name = "DISTRITO")
    private Long distrito;

    @Id
    @Column(name = "LOGRADOURO")
    private Long logradouro;

    @Id
    @Column(name = "NUMERO_INI")
    private Long numero_ini;

    @Column(name = "CEP")
    private Long cep;

    @Column(name = "NUMERO_FIM")
    private Long numero_fim;

    @Column(name = "IDENTIFICACAO")
    private String identificacao;

    // 🔥 FK para LOGRADOURO (correto)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false),
            @JoinColumn(name = "LOGRADOURO", referencedColumnName = "LOGRADOURO", insertable = false, updatable = false)
    })
    private PesLogradouro pesLogradouro;
}