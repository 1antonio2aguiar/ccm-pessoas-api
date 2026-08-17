package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.DadosPessoaJuridica;
import br.com.cc.pessoas.repository.estabelecimento.EstabelecimentoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstabelecimentoRepository
        extends JpaRepository<DadosPessoaJuridica, Long>,
        EstabelecimentoRepositoryQuery {

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(
            String cnpj,
            Long id
    );
}