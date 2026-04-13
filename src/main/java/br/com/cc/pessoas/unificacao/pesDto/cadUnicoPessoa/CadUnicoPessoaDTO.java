package br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa;

import br.com.cc.pessoas.unificacao.pesEntity.CadUnicoPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.PesCidade;

import java.time.LocalDateTime;

public record CadUnicoPessoaDTO(
        Long id,
        Long cdOrigem,
        Long tipoPessoa,
        String nome,
        String fisicaJuridica,
        Long cpfCnpj,
        String estadoCivil,
        String sexo,
        String email,
        String banco,
        Long pessoasCdUnico,
        String status,
        LocalDateTime dataNascimento,
        LocalDateTime dataCadastro,
        String observacao,
        Long cidadeNascimento,
        String cidadeNascimentoNome
) {
    public static CadUnicoPessoaDTO fromEntity(CadUnicoPessoa entity) {
        PesCidade cidadeNascimento = entity.getPesCidadeNascimento();

        return new CadUnicoPessoaDTO(
                entity.getId(),
                entity.getCdOrigem(),
                entity.getTipoPessoa(),
                entity.getNome(),
                entity.getFisicaJuridica(),
                entity.getCpfCnpj(),
                entity.getEstadoCivil(),
                entity.getSexo(),
                entity.getEmail(),
                entity.getBanco(),
                entity.getPessoasCdUnico(),
                entity.getStatus(),
                entity.getDataNascimento(),
                entity.getDataCadastro(),
                entity.getObservacao(),
                cidadeNascimento != null ? cidadeNascimento.getCidade() : null,
                cidadeNascimento != null ? cidadeNascimento.getNome() : null
        );
    }
}