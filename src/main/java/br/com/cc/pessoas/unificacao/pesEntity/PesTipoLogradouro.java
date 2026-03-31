package br.com.cc.pessoas.unificacao.pesEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PES_TIPOS_LOGRADOUROS", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PesTipoLogradouro {

    @Id
    @Column(name = "TIPO_LOGRADOURO")
    private String tipoLogradouro;

    @Column(name = "DESCRICAO")
    private String descricao;
}
