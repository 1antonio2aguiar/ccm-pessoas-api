package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DADOS_PJ", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DadosPessoaJuridica {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dados_pj_seq")
    @SequenceGenerator(
            name = "dados_pj_seq",
            sequenceName = "SEQ_DADOS_PJ",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PESSOA_ID", nullable = false)
    private Pessoa pessoa;

    @Column(name = "CNPJ", length = 15, unique = true)
    private String cnpj;

    @Column(name = "NOME", length = 120)
    private String nome;

    @Column(name = "ESTABELECIMENTO")
    private Long estabelecimento;

    @Column(name = "NOME_FANTASIA", length = 255)
    private String nomeFantasia;

    @Column(name = "OBJETO_SOCIAL", length = 1000)
    private String objetoSocial;

    @Column(name = "MICRO_EMPRESA", length = 1)
    private String microEmpresa;

    @Column(name = "CONJUGE", length = 80)
    private String conjuge;

    @Column(name = "MES_ENVIO_SICOM")
    private Integer mesEnvioSicom;

    @Column(name = "ANO_ENVIO_SICOM")
    private Integer anoEnvioSicom;

    @Column(name = "TIPO_EMPRESA")
    private Long tipoEmpresa;
}