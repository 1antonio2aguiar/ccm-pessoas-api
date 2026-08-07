package br.com.cc.pessoas.dto.pessoa;

import java.time.LocalDateTime;
public record PessoaCpfCnpjCadUnicoDTO(
        boolean existe,
        Long pessoaId,
        String nome,
        String cpfCnpj,
        LocalDateTime dataNascimento
) {

    public static PessoaCpfCnpjCadUnicoDTO naoEncontrado() {
        return new PessoaCpfCnpjCadUnicoDTO(
                false,
                null,
                null,
                null,
                null
        );
    }
}