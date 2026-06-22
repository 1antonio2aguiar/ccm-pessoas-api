package br.com.cc.pessoas.unificacao.saneamento.saneRepository;

import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.unificacao.rh.rhEntity.RhPessoa;
import br.com.cc.pessoas.unificacao.rh.rhRepository.rhPessoa.RhPessoaRepositoryCustom;
import br.com.cc.pessoas.unificacao.saneamento.saneEntity.SanePessoa;
import br.com.cc.pessoas.unificacao.saneamento.saneRepository.SaniPessoaRepository.SanePessoaRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SanePessoaRepository extends JpaRepository<SanePessoa, Long>, SanePessoaRepositoryCustom {
}