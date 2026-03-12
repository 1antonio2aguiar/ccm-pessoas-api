package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Endereco;
import br.com.cc.pessoas.repository.endereco.EnderecoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnderecoRepository extends JpaRepository<Endereco, Long>, EnderecoRepositoryQuery {
    //Optional<Endereco> findByCepId(String cep);
    List<Endereco> findByPessoaId(Long pessoaId);


    @Modifying
    @Query("UPDATE Endereco e SET e.principal = 'N' WHERE e.pessoa.id = :pessoaId")
    int marcarTodosComoNaoPrincipalParaPessoa(@Param("pessoaId") Long pessoaId);
}
