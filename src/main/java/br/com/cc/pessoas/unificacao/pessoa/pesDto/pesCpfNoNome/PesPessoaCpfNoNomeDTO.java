package br.com.cc.pessoas.unificacao.pessoa.pesDto.pesCpfNoNome;
public record PesPessoaCpfNoNomeDTO(
        Long pessoa,
        String nome,
        String cpfNoNome,
        Long cgcCpf,
        String nomeSemCpf,
        String posicaoCpf
) {
}
