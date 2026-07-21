package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Contato;
import br.com.cc.pessoas.entity.enuns.TipoContato;
import br.com.cc.pessoas.repository.contato.ContatoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContatoRepository
        extends JpaRepository<Contato, Long>, ContatoRepositoryQuery {

    List<Contato> findByPessoaId(Long pessoaId);

    @Modifying
    @Query("""
        UPDATE Contato c
           SET c.principal = 'N'
         WHERE c.pessoa.id = :pessoaId
           AND c.dadosPessoaJuridica IS NULL
           AND c.tipoContato = :tipoContato
           AND (
                :contatoId IS NULL
                OR c.id <> :contatoId
           )
        """)
    int marcarTodosComoNaoPrincipalParaPessoaFisica(
            @Param("pessoaId") Long pessoaId,
            @Param("tipoContato") TipoContato tipoContato,
            @Param("contatoId") Long contatoId
    );

    @Modifying
    @Query("""
        UPDATE Contato c
           SET c.principal = 'N'
         WHERE c.pessoa.id = :pessoaId
           AND c.dadosPessoaJuridica.id = :dadosPessoaJuridicaId
           AND c.tipoContato = :tipoContato
           AND (
                :contatoId IS NULL
                OR c.id <> :contatoId
           )
        """)
    int marcarTodosComoNaoPrincipalParaEstabelecimento(
            @Param("pessoaId") Long pessoaId,
            @Param("dadosPessoaJuridicaId") Long dadosPessoaJuridicaId,
            @Param("tipoContato") TipoContato tipoContato,
            @Param("contatoId") Long contatoId
    );
}