package br.com.cc.pessoas.unificacao.pesRepository.pesTipoDocumento;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoDocumento;
import br.com.cc.pessoas.unificacao.pesFilter.PesTipoDocumentoFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PesTipoDocumentoRepositoryQuery {
    Page<PesTipoDocumento> filtrar(PesTipoDocumentoFilter filter, Pageable pageable);

    List<PesTipoDocumento> filtrar(PesTipoDocumentoFilter filter);
}
