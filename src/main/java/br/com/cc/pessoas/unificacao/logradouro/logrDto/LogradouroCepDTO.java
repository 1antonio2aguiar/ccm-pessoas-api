package br.com.cc.pessoas.unificacao.logradouro.logrDto;

/*
 * CEP válido da tabela de CEPs dos Correios (CEPS_CARGA_DNE),
 * oferecido para o logradouro criado pela tela.
 *
 * A tela nunca cria um valor de CEP: só escolhe um destes.
 */
public record LogradouroCepDTO(
        Long cep,
        Long distrito,
        Long logradouro,
        String nomeLogradouro,
        Long bairro,
        String nomeBairro,
        Long numeroIni,
        Long numeroFim,
        String complemento
) {
}
