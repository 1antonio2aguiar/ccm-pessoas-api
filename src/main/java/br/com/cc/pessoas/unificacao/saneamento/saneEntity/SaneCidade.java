package br.com.cc.pessoas.unificacao.saneamento.saneEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SANE_CIDADES", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaneCidade {

    @Id
    @Column(name = "CIDADE")
    private Long cidade;
    private String nome;
    private Long cep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ESTADO", nullable = false)
    private SaneEstado estado;
}
