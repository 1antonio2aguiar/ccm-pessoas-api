package br.com.cc.pessoas.unificacao.rh.rhEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "RH_PESSOAS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class RhPessoa {

    @Id
    @Column(name = "PESSOA")
    private Long pessoa;

    @Column(name = "NOME", nullable = false)
    private String nome;

    @Column(name = "DATA_CADASTRO")
    private LocalDateTime dataCadastro;

    @Column(name = "FISICA_JURIDICA", length = 1)
    private String fisicaJuridica;

    @Column(name = "CGC_CPF")
    private Long cgcCpf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TIPO_PESSOA", referencedColumnName = "TIPO_PESSOA")
    private RhTipoPessoa rhTipoPessoa;

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
    private String complemento;

    @Column(name = "CEP")
    private Long cep;

    @Column(name = "DATA_NASCIMENTO")
    private LocalDateTime dataNascimento;

    @Column(name = "ESTADO_CIVIL")
    private String estadoCivil;

    @Column(name = "SEXO")
    private String sexo;

    @Column(name = "CIDADE_NASCIMENTO")
    private Long cidadeNascimento;

    @Column(name = "PAIS")
    private Long pais;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TIPO_DOCUMENTO", referencedColumnName = "TIPO_DOCUMENTO")
    private RhTipoDocumento rhTipoDocumento;

    @Column(name = "NUMERO_DOCTO")
    private String numeroDocto;

    @Column(name = "ORGAO_DOCTO")
    private String orgaoDocto;

    @Column(name = "EMISSAO_DOCTO")
    private LocalDateTime emissaoDocto;

    @Column(name = "TITULO_ELEITORAL")
    private Long tituloEleitoral;

    @Column(name = "ZONA")
    private Long zona;

    @Column(name = "SECAO")
    private Long secao;

    @Column(name = "MAE")
    private String mae;

    @Column(name = "PAI")
    private String pai;

    @Column(name = "DDD_TELEFONE")
    private Long dddTelefone;

    @Column(name = "TELEFONE")
    private Long telefone;

    @Column(name = "DDD_RECADO")
    private Long dddRecado;

    @Column(name = "RECADO")
    private Long recado;

    @Column(name = "DDD_CELULAR")
    private Long dddCelular;

    @Column(name = "CELULAR")
    private Long celular;

    @Column(name = "WHATSAPP")
    private Long whatsapp;

    @Column(name = "FAX")
    private Long fax;

    @Column(name = "E_MAIL")
    private String email;

    @Column(name = "PAGINA_WEB")
    private String paginaWeb;

    @Column(name = "PESSOA_MATRIZ")
    private Long pessoaMatriz;

    @Column(name = "INSCRICAO_ESTADUAL")
    private String insricaoEstadual;

    @Column(name = "FANTASIA")
    private String fantasia;

    @Column(name = "PROFISSAO")
    private Long profissao;

    @Column(name = "VIP")
    private String vip;

    @Column(name = "NOME_CONJUGE")
    private String conjuge;

    @Column(name = "MES_ENVIO_SICOM")
    private Long mesEnvioSicom;

    @Column(name = "NOME_SOCIAL")
    private String nomeSocial;

    @Column(name = "INSTAGRAM")
    private String instagram;

    @Column(name = "FACEBOOK")
    private String facebook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false)
    private RhCidade rhCidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false)
    })
    private RhDistrito rhDistrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false),
            @JoinColumn(name = "BAIRRO", referencedColumnName = "BAIRRO", insertable = false, updatable = false)
    })
    private RhBairro rhBairro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "CIDADE", referencedColumnName = "CIDADE", insertable = false, updatable = false),
            @JoinColumn(name = "DISTRITO", referencedColumnName = "DISTRITO", insertable = false, updatable = false),
            @JoinColumn(name = "LOGRADOURO", referencedColumnName = "LOGRADOURO", insertable = false, updatable = false)
    })
    private RhLogradouro rhLogradouro;

    @Transient
    public String getBairroNome() {
        return this.rhBairro != null ? this.rhBairro.getNome() : null;
    }

    @Transient
    public String getLogradouroNome() {
        return this.rhLogradouro != null ? this.rhLogradouro.getNome() : null;
    }
}