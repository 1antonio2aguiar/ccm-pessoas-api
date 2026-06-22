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
@Table(name = "SANE_TIPOS_LOGRADOUROS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaneTipoLogradouro {

    @Id
    @Column(name = "TIPO_LOGRADOURO")
    private String tipoLogradouro;

    @Column(name = "DESCRICAO")
    private String descricao;
}
