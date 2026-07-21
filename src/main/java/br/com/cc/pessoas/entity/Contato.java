package br.com.cc.pessoas.entity;

import br.com.cc.pessoas.entity.enuns.TipoContato;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CONTATOS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contato {

    @Id
    @GeneratedValue(generator = "SEQ_CONTATOS")
    @SequenceGenerator(
            name = "SEQ_CONTATOS",
            sequenceName = "SEQ_CONTATOS",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "TIPO_CONTATO")
    private TipoContato tipoContato;

    @Column(name = "CONTATO")
    private String contato;

    @Column(name = "PRINCIPAL")
    private String principal;

    @Column(name = "COMPLEMENTO")
    private String complemento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PESSOA_ID", nullable = false)
    private Pessoa pessoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DADOS_PJ_ID")
    private DadosPessoaJuridica dadosPessoaJuridica;
}