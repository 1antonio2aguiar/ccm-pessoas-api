package br.com.cc.pessoas.unificacao.pesRepository.cadUnicoPessoa;

import br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa.CadUnicoPessoaOrigemDTO;
import java.util.List;
public interface CadUnicoPessoaRepositoryCustom {
    List<CadUnicoPessoaOrigemDTO> buscarOrigens(Long pessoasCdUnico);
}
