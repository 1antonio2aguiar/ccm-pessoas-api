package br.com.cc.pessoas.unificacao.pesRepository;

import br.com.cc.pessoas.unificacao.pesEntity.PesTipoDocumento;
import br.com.cc.pessoas.unificacao.pesRepository.pesTipoDocumento.PesTipoDocumentoRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesTipoDocumentoRepository
        extends JpaRepository<PesTipoDocumento, Integer>, PesTipoDocumentoRepositoryQuery {
}
