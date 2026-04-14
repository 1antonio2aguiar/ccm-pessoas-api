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
@PrimaryKeyJoinColumn(name = "id")
public class DadosPessoaFisica extends Pessoa {

    @Column(name = "cpf", length = 11, unique = true)
    private String cpf;

    @Column(name = "nome_social")
    private String nomeSocial;

    @Column(name = "raca")
    private String raca;

    @Column(name = "etnia")
    private String etnia;

    @Column(name = "cor")
    private String cor;

    @Column(name = "recebe_bf")
    private String recebeBf;

    @Column(name = "cartao_sus")
    private String cartaoSus;

    @Column(name = "sexo")
    private String sexo;

    @Column(name = "estado_civil")
    private EstadoCivil estadoCivil;

    @Column(name = "local_nascimento_id")
    private Long localNascimentoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_nascimento_id", insertable = false, updatable = false)
    private Cidade localNascimento;

    @Column(name = "mae")
    private String mae;

    @Column(name = "pai")
    private String pai;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

}