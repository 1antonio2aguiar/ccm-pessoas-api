package br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa;

import java.time.LocalDateTime;

import br.com.cc.pessoas.unificacao.pesEntity.CadUnicoPessoa;

import java.time.LocalDateTime;

public record CadUnicoPessoaCreateDTO(

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
        Long cidadeNascimento

) {

    public CadUnicoPessoa toEntity() {
        CadUnicoPessoa entity = new CadUnicoPessoa();

        entity.setCdOrigem(this.cdOrigem);
        entity.setTipoPessoa(this.tipoPessoa);
        entity.setNome(this.nome);
        entity.setFisicaJuridica(this.fisicaJuridica);
        entity.setCpfCnpj(this.cpfCnpj);
        entity.setEstadoCivil(this.estadoCivil);
        entity.setSexo(this.sexo);
        entity.setEmail(this.email);
        entity.setBanco(this.banco);
        entity.setPessoasCdUnico(this.pessoasCdUnico);
        entity.setStatus(this.status);
        entity.setDataNascimento(this.dataNascimento);
        entity.setDataCadastro(this.dataCadastro);
        entity.setObservacao(this.observacao);;

        return entity;
    }
}