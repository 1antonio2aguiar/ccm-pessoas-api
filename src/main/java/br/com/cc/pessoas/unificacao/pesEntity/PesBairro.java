package br.com.cc.pessoas.unificacao.pesEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "PES_BAIRROS", schema = "DBO_CCM_PESSOAS")
@IdClass(PesBairroId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PesBairro {

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

    @Column(name = "DATA_CRIACAO")
    private LocalDate dataCriacao;

    @Column(name = "LEI_CRIACAO")
    private String leiCriacao;

    @Column(name = "DATA_DECRETO")
    private LocalDate dataDecreto;

    @Column(name = "DECRETO")
    private Long decreto;

    @Column(name = "DATA_PORTARIA")
    private LocalDate dataPortaria;

    @Column(name = "PORTARIA")
    private Long portaria;

    @Column(name = "NOME_VEREADOR")
    private String nomeVereador;

    @Column(name = "PESSOA_LOTEADORA")
    private Long pessoaLoteadora;

    @Column(name = "TIPO_BAIRRO")
    private String tipoBairro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false)
    })
    private PesDistrito pesDistrito;
}