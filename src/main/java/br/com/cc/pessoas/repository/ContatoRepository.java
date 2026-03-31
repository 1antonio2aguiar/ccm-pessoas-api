package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Contato;
import br.com.cc.pessoas.repository.contato.ContatoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContatoRepository extends JpaRepository<Contato, Long>, ContatoRepositoryQuery {
    List<Contato> findByPessoaId(Long pessoaId);

    @Modifying
    @Query("UPDATE Contato e SET e.principal = 'N' WHERE e.pessoa.id = :pessoaId")
    int marcarTodosComoNaoPrincipalParaPessoa(@Param("pessoaId") Long pessoaId);
}
