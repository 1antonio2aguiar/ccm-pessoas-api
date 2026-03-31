package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pesEntity.CadUnicoPessoa;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesRepository.cadUnicoPessoa.CadUnicoPessoaRepositoryQuery;
import br.com.cc.pessoas.unificacao.pesRepository.pesPessoa.PesPessoaRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CadUnicoPessoaRepository
        extends JpaRepository<CadUnicoPessoa, Long>, CadUnicoPessoaRepositoryQuery {
}
