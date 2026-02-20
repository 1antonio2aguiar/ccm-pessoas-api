package br.com.cc.pessoas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "DADOS_PJ", schema = "DBO_CCM_PESSOAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class DadosPessoaJuridica extends Pessoa {

    @Column(name = "cnpj", length = 14, unique = true)
    private String cnpj;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @Column(name = "objeto_social")
    private String objetoSocial;

    @Column(name = "micro_empresa", length = 1)
    private String microEmpresa;

    @Column(name = "conjuge")
    private String conjuge;

    @Column(name = "tipo_empresa")
    private String tipoEmpresa;
}