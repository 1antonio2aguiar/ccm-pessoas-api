package br.com.cc.pessoas.unificacao.pesEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "CAD_UNICO_PESSOA", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class CadUnicoPessoa {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "CD_ORIGEM")
    private Long cdOrigem;

    @Column(name = "TIPO_PESSOA")
    private Long tipoPessoa;

    @Column(name = "NOME", nullable = false)
    private String nome;

    @Column(name = "FISICA_JURIDICA", length = 1)
    private String fisicaJuridica;

    @Column(name = "CPF_CNPJ")
    private Long cpfCnpj;

    @Column(name = "ESTADO_CIVIL")
    private String estadoCivil;

    @Column(name = "SEXO")
    private String sexo;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "BANCO")
    private String banco;

    @Column(name = "PESSOAS_CD_UNICO")
    private Long pessoasCdUnico;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "DATA_NASCIMENTO")
    private LocalDateTime dataNascimento;

    @Column(name = "DATA_CADASTRO")
    private LocalDateTime dataCadastro;

    @Column(name = "OBSERVACAO")
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CIDADE_NASCIMENTO", referencedColumnName = "CIDADE", insertable = false, updatable = false)
    private PesCidade pesCidadeNascimento;
}