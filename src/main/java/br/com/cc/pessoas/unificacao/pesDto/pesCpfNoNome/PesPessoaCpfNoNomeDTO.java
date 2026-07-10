package br.com.cc.pessoas.unificacao.pesDto.pesCpfNoNome;
public record PesPessoaCpfNoNomeDTO(
        Long pessoa,
        String nome,
        String cpfNoNome,
        Long cgcCpf,
        String nomeSemCpf,
        String posicaoCpf
) {
}
