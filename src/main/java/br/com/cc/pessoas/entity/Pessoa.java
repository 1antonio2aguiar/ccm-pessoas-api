package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PESSOAS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pessoa_seq")
    @SequenceGenerator(
            name = "pessoa_seq",
            sequenceName = "SEQ_PESSOAS",
            schema = "DBO_CCM_PESSOAS",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "TIPO_PESSOA_ID")
    private Long tipoPessoaId;

    @Column(name = "NOME", nullable = false)
    private String nome;

    @Column(name = "FISICA_JURIDICA", length = 1)
    private String fisicaJuridica;

    @Column(name = "DATA_CADASTRO")
    private LocalDateTime dataCadastro;

    @Column(name = "OBSERVACAO")
    private String observacao;

    @Column(name = "SITUACAO_ID")
    private Long situacaoId;

    @OneToOne(mappedBy = "pessoa", fetch = FetchType.LAZY)
    private DadosPessoaFisica dadosPessoaFisica;

    @OneToMany(mappedBy = "pessoa", fetch = FetchType.LAZY)
    private List<DadosPessoaJuridica> dadosPessoasJuridicas = new ArrayList<>();
}