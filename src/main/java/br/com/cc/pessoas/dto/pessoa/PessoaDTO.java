package br.com.cc.pessoas.dto.pessoa;

import br.com.cc.pessoas.entity.DadosPessoaFisica;
import br.com.cc.pessoas.entity.DadosPessoaJuridica;
import br.com.cc.pessoas.entity.Pessoa;

import java.time.LocalDateTime;
import java.util.Optional;

public record PessoaDTO(

        Long id,
        String nome,
        String fisicaJuridica,

        Long tipoPessoaId,
        Long situacaoId,

        LocalDateTime dataCadastro,
        String observacao,

        DadosPessoaFisicaDTO dadosPessoaFisica,
        DadosPessoaJuridicaDTO dadosPessoaJuridica

) {

    public static PessoaDTO fromPessoa(Pessoa pessoa) {

        if (pessoa == null) {
            return null;
        }

        DadosPessoaFisicaDTO pfDTO = (pessoa instanceof DadosPessoaFisica pf)
                ? DadosPessoaFisicaDTO.fromEntity(pf)
                : null;

        DadosPessoaJuridicaDTO pjDTO = (pessoa instanceof DadosPessoaJuridica pj)
                ? DadosPessoaJuridicaDTO.fromEntity(pj)
                : null;

        return new PessoaDTO(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getFisicaJuridica(),
                pessoa.getTipoPessoaId(),
                pessoa.getSituacaoId(),
                pessoa.getDataCadastro(),
                pessoa.getObservacao(),
                pfDTO,
                pjDTO
        );
    }

    public static PessoaDTO fromOptionalPessoa(Optional<Pessoa> pessoa) {
        return pessoa.map(PessoaDTO::fromPessoa).orElse(null);
    }
}
