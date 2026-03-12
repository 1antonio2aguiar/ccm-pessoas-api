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

    private TipoEndereco tipoEndereco;
    private Long numero;
    private String complemento;
    private String principal;
    private String banco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cep_id", nullable = false)
    private Cep cep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logradouro_id", nullable = false)
    private Logradouro logradouro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bairro_id", nullable = false)
    private Bairro bairro;
}
