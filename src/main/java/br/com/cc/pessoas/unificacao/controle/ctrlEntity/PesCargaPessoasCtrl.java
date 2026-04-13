package br.com.cc.pessoas.unificacao.controle.ctrlEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "CARGA_PESSOAS_CTRL", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
public class PesCargaPessoasCtrl {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "TOTAL_PROCESSADO")
    private Long totalProcessado;

    @Column(name = "TOTAL_ERROS")
    private Long totalErros;

    @Column(name = "DATA_INICIO")
    private LocalDateTime dataInicio;

    @Column(name = "DATA_FIM")
    private LocalDateTime dataFim;

    @Column(name = "MENSAGEM_ERRO")
    private String mensagemErro;
}