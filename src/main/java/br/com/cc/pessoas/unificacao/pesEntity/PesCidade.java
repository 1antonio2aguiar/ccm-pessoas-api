package br.com.cc.pessoas.unificacao.pesEntity;

import br.com.cc.pessoas.entity.Estado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PES_CIDADES", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PesCidade {

    @Id
    @Column(name = "CIDADE")
    private Long cidade;
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ESTADO", nullable = false)
    private PesEstado estado;
}
