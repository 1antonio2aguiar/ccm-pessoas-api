package br.com.cc.pessoas.unificacao.rh.rhRepository;

import br.com.cc.pessoas.unificacao.rh.rhEntity.RhPessoa;
import br.com.cc.pessoas.unificacao.rh.rhRepository.rhPessoa.RhPessoaRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RhPessoaRepository extends JpaRepository<RhPessoa, Long>, RhPessoaRepositoryCustom {
}