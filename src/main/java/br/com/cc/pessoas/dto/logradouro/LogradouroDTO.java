package br.com.cc.pessoas.dto.logradouro;

import br.com.cc.pessoas.entity.Logradouro;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LogradouroDTO {

    private Long id;
    private String nome;
    private String preposicao;
    private String tituloPatente;
    private String nomeReduzido;
    private String nomeSimplificado;
    private String complemento;

    private Long tipoLogradouroId;
    private String tipoLogradouro;
    private String tipoLogradouroDescricao;

    private Long distritoId;
    private String nomeDistrito;

    private Long cidadeId;
    private String nomeCidade;

    public static LogradouroDTO fromLogradouro(Logradouro logradouro) {
        return new LogradouroDTO(
                logradouro.getId(),
                logradouro.getNome(),
                logradouro.getPreposicao(),
                logradouro.getTituloPatente(),
                logradouro.getNomeReduzido(),
                logradouro.getNomeSimplificado(),
                logradouro.getComplemento(),
                logradouro.getTipoLogradouro().getId(),
                logradouro.getTipoLogradouro().getSigla(),
                logradouro.getTipoLogradouro().getDescricao(),
                logradouro.getDistrito().getId(),
                logradouro.getDistrito().getNome(),
                logradouro.getDistrito().getCidade().getId(),
                logradouro.getDistrito().getCidade().getNome()
        );
    }
}