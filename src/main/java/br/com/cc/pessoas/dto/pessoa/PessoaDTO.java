package br.com.cc.pessoas.dto.pessoa;

import br.com.cc.pessoas.entity.Pessoa;

import java.time.LocalDateTime;
import java.util.List;
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
        List<DadosPessoaJuridicaDTO> dadosPessoasJuridicas

) {

    public static PessoaDTO fromPessoa(Pessoa pessoa) {

        if (pessoa == null) {
            return null;
        }

        DadosPessoaFisicaDTO pfDTO =
                DadosPessoaFisicaDTO.fromEntity(pessoa.getDadosPessoaFisica());

        List<DadosPessoaJuridicaDTO> pjDTOs =
                pessoa.getDadosPessoasJuridicas() == null
                        ? List.of()
                        : pessoa.getDadosPessoasJuridicas()
                        .stream()
                        .map(DadosPessoaJuridicaDTO::fromEntity)
                        .toList();

        return new PessoaDTO(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getFisicaJuridica(),
                pessoa.getTipoPessoaId(),
                pessoa.getSituacaoId(),
                pessoa.getDataCadastro(),
                pessoa.getObservacao(),
                pfDTO,
                pjDTOs
        );
    }

    public static PessoaDTO fromOptionalPessoa(Optional<Pessoa> pessoa) {
        return pessoa.map(PessoaDTO::fromPessoa).orElse(null);
    }
}