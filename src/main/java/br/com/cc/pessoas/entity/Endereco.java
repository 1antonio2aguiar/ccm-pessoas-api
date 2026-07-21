package br.com.cc.pessoas.entity;

import br.com.cc.pessoas.entity.enuns.TipoEndereco;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ENDERECOS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Endereco {

    @Id
    @GeneratedValue(generator = "SEQ_ENDERECOS")
    @SequenceGenerator(
            name = "SEQ_ENDERECOS",
            sequenceName = "SEQ_ENDERECOS",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "TIPO_ENDERECO")
    private TipoEndereco tipoEndereco;

    @Column(name = "NUMERO")
    private Long numero;

    @Column(name = "COMPLEMENTO")
    private String complemento;

    @Column(name = "PRINCIPAL")
    private String principal;

    @Column(name = "BANCO")
    private String banco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PESSOA_ID", nullable = false)
    private Pessoa pessoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DADOS_PJ_ID")
    private DadosPessoaJuridica dadosPessoaJuridica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CEP_ID", nullable = false)
    private Cep cep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOGRADOURO_ID", nullable = false)
    private Logradouro logradouro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BAIRRO_ID", nullable = false)
    private Bairro bairro;
}