package br.com.cc.pessoas.repository.documento;

import br.com.cc.pessoas.entity.Documento;
import br.com.cc.pessoas.filter.DocumentoFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DocumentoRepositoryQuery {
    List<Documento> filtrar(DocumentoFilter filter);

    Page<Documento> filtrar(DocumentoFilter filter, Pageable pageable);
}
