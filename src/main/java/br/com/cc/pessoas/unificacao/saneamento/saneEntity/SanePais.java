package br.com.cc.pessoas.unificacao.saneamento.saneEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SANE_PAISES", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SanePais {

    @Id
    @Column(name = "PAIS")
    private Long pais;
    private String nome;
    private String nacionalidade;
}
