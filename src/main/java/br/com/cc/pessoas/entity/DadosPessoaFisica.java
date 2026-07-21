package br.com.cc.pessoas.entity;

import br.com.cc.pessoas.entity.enuns.EstadoCivil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "DADOS_PF", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DadosPessoaFisica {

    @Id
    @Column(name = "ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "ID")
    private Pessoa pessoa;

    @Column(name = "CPF", length = 11, unique = true)
    private String cpf;

    @Column(name = "NOME_SOCIAL")
    private String nomeSocial;

    @Column(name = "RACA")
    private String raca;

    @Column(name = "ETNIA")
    private String etnia;

    @Column(name = "COR")
    private String cor;

    @Column(name = "RECEBE_BF")
    private String recebeBf;

    @Column(name = "CARTAO_SUS")
    private String cartaoSus;

    @Column(name = "SEXO")
    private String sexo;

    @Column(name = "ESTADO_CIVIL")
    private EstadoCivil estadoCivil;

    @Column(name = "LOCAL_NASCIMENTO_ID")
    private Long localNascimentoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "LOCAL_NASCIMENTO_ID",
            insertable = false,
            updatable = false
    )
    private Cidade localNascimento;

    @Column(name = "MAE")
    private String mae;

    @Column(name = "PAI")
    private String pai;

    @Column(name = "DATA_NASCIMENTO")
    private LocalDate dataNascimento;
}