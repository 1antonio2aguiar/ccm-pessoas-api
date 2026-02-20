package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "PESSOAS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pessoa_seq")
    @SequenceGenerator(name = "pessoa_seq", sequenceName = "SEQ_PESSOAS", allocationSize = 1)
    private Long id;

    @Column(name = "tipo_pessoa_id")
    private Long tipoPessoaId;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "fisica_juridica", length = 1)
    private String fisicaJuridica; // F ou J

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "situacao_id")
    private Long situacaoId;
}