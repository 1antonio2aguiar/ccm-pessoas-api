package br.com.cc.pessoas.unificacao.pesEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PES_PESSOAS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class PesPessoa {

    @Id
    @Column(name = "PESSOA")
    private Long pessoa;

    @Column(name = "NOME", nullable = false)
    private String nome;

    @Column(name = "FISICA_JURIDICA", length = 1)
    private String fisicaJuridica;

    @Column(name = "CGC_CPF")
    private Long cgcCpf;

    @Column(name = "TIPO_PESSOA")
    private Long tipoPessoa;

    @Column(name = "CIDADE")
    private Long cidade;

    @Column(name = "DISTRITO")
    private Long distrito;

    @Column(name = "BAIRRO")
    private Long bairro;

    @Column(name = "LOGRADOURO")
    private Long logradouro;

    @Column(name = "NUMERO")
    private Long numero;

    @Column(name = "COMPLEMENTO")
    private Long complemento;

    @Column(name = "CEP")
    private Long cep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TIPO_DOCUMENTO", referencedColumnName = "TIPO_DOCUMENTO")
    private PesTipoDocumento tipoDocumento;

    @Column(name = "NUMERO_DOCTO")
    private Long numeroDocto;

    @Column(name = "ORGAO_DOCTO")
    private Long orgaoDocto;

    @Column(name = "DATA_DOCTO")
    private LocalDateTime dataDocto;

    @Column(name = "TELEFONE")
    private Long telefone;

    @Column(name = "RECADO")
    private Long recado;

    @Column(name = "CELULAR")
    private Long celular;

    @Column(name = "FAX")
    private Long fax;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "PAGINA_WEB")
    private String paginaWeb;

    @Column(name = "VIP")
    private String vip;

    @Column(name = "SEXO")
    private String sexo;

    @Column(name = "ESTADO_CIVIL")
    private String estadoCivil;

    @Column(name = "DATA_NASCIMENTO")
    private LocalDateTime dataNascimento;

    @Column(name = "PAI")
    private String pai;

    @Column(name = "MAE")
    private String mae;

    @Column(name = "NOME_SOCIAL")
    private String nomeSocial;

    @Column(name = "ZONA")
    private Long zona;

    @Column(name = "SECAO")
    private Long secao;

    @Column(name = "TIPO_EMPRESA")
    private Long tipoEmpresa;

    @Column(name = "PESSOA_MATRIZ")
    private Long pessoaMatriz;

    @Column(name = "INSCRICAO_ESTADUAL")
    private String insricaoEstadual;

    @Column(name = "FANTASIA")
    private String fantasia;

    @Column(name = "OBJETO_SOCIAL")
    private String objetoSocial;

    @Column(name = "MICRO_EMPRESA")
    private String microEmpresa;

    @Column(name = "CONJUGE")
    private Long conjuge;

    @Column(name = "PROFISSAO")
    private Long profissao;

    @Column(name = "CIDADE_NASCIMENTO")
    private Long cidadeNascimento;

    @Column(name = "DATA_CADASTRO")
    private LocalDateTime dataCadastro;

    @Column(name = "OBSERVACAO")
    private String observacao;

    @Column(name = "USUARIO")
    private Long usuario;

    @Column(name = "USUARIO_ALTERACAO")
    private Long usuarioAlteracao;

    @Column(name = "MES_ENVIO_SICOM")
    private Long mesEnvioSicom;

    @Column(name = "ANO_ENVIO_SICOM")
    private Long anoEnvioSicom;

    @Column(name = "DEFICIENTE")
    private String deficiente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false)
    private PesCidade pesCidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false)
    })
    private PesDistrito pesDistrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false),
            @JoinColumn(name = "BAIRRO", referencedColumnName = "BAIRRO", insertable = false, updatable = false)
    })
    private PesBairro pesBairro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false),
            @JoinColumn(name = "LOGRADOURO", referencedColumnName = "LOGRADOURO", insertable = false, updatable = false)
    })
    private PesLogradouro pesLogradouro;
}