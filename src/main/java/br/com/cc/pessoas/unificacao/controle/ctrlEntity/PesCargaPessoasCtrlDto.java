package br.com.cc.pessoas.unificacao.controle.ctrlEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PesCargaPessoasCtrlDto {

    private Long id;
    private String status;
    private Long totalProcessado;
    private Long totalErros;
    private String mensagemErro;
}