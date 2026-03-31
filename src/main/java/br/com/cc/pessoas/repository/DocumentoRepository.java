package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.Documento;
import br.com.cc.pessoas.repository.documento.DocumentoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long>, DocumentoRepositoryQuery {
    List<Documento> findByPessoaId(Long pessoaId);

}
