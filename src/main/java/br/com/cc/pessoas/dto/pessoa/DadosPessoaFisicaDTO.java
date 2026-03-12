package br.com.cc.pessoas.dto.pessoa;

import br.com.cc.pessoas.entity.DadosPessoaFisica;

import java.time.LocalDate;
import java.util.Optional;

public record DadosPessoaFisicaDTO(

        String cpf,
        String nomeSocial,
        String raca,
        String etnia,
        String cor,
        String recebeBf,
        String cartaoSus,
        String sexo,
        String estadoCivil,
        Long localNascimentoId,
        String mae,
        String pai,
        LocalDate dataNascimento

) {

    public static DadosPessoaFisicaDTO fromEntity(DadosPessoaFisica pf) {

        if (pf == null) {
            return null;
        }

        return new DadosPessoaFisicaDTO(
                pf.getCpf(),
                pf.getNomeSocial(),
                pf.getRaca(),
                pf.getEtnia(),
                pf.getCor(),
                pf.getRecebeBf(),
                pf.getCartaoSus(),
                pf.getSexo(),
                pf.getEstadoCivil(),
                pf.getLocalNascimentoId(),
                pf.getMae(),
                pf.getPai(),
                pf.getDataNascimento()
        );
    }

    public static DadosPessoaFisicaDTO fromOptional(Optional<DadosPessoaFisica> pf) {
        return pf.map(DadosPessoaFisicaDTO::fromEntity).orElse(null);
    }
}
