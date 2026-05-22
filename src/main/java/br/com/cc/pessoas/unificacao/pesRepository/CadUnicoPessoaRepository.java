package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pesEntity.CadUnicoPessoa;
import br.com.cc.pessoas.unificacao.pesRepository.cadUnicoPessoa.CadUnicoPessoaRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CadUnicoPessoaRepository
        extends JpaRepository<CadUnicoPessoa, Long>, CadUnicoPessoaRepositoryCustom {
}