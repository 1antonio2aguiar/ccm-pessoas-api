package br.com.cc.pessoas.entity;

import br.com.cc.pessoas.entity.enuns.TipoDocumento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "DOCUMENTOS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(generator = "SEQ_DOCUMENTOS")
    @SequenceGenerator(
            name = "SEQ_DOCUMENTOS",
            sequenceName = "SEQ_DOCUMENTOS",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "tipo_documento")
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Column(name = "data_documento")
    private LocalDate dataDocumento;

    @Column(name = "data_expedicao")
    private LocalDate dataExpedicao;

    @Column(name = "documento_origem")
    private String documentoOrigem;

    @Column(name = "orgao_expedidor")
    private String orgaoExpedidor;

    @Column(name = "data_primeira_cnh")
    private LocalDate dataPrimeiraCnh;

    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Column(name = "categoria_cnh")
    private String categoriaCnh;

    @Column(name = "zona")
    private Long zona;

    @Column(name = "secao")
    private Long secao;

    @Column(name = "observacao")
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

}
