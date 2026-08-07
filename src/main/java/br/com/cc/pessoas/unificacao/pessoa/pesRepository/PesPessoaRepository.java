package br.com.cc.pessoas.unificacao.pessoa.pesRepository;

import br.com.cc.pessoas.unificacao.pesRepository.pesCpfNoNome.PesPessoaCpfNoNomeRepositoryCustom;
import br.com.cc.pessoas.unificacao.pesRepository.pesPessoa.PesPessoaRepositoryQuery;
import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pessoa.pesRepository.pesPessoa.PesPessoaRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesPessoaRepository
        extends JpaRepository<PesPessoa, Long>,
        PesPessoaRepositoryCustom {
}

/*public interface PesPessoaRepository
        extends JpaRepository<PesPessoa, Long>,
        PesPessoaRepositoryQuery,
        PesPessoaRepositoryCustom,
        PesPessoaCpfNoNomeRepositoryCustom {
}*/