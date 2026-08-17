package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.documento.DocumentoCreateDTO;
import br.com.cc.pessoas.dto.documento.DocumentoDTO;
import br.com.cc.pessoas.dto.documento.DocumentoUpdateDTO;
import br.com.cc.pessoas.entity.DadosPessoaJuridica;
import br.com.cc.pessoas.entity.Documento;
import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.entity.enuns.TipoDocumento;
import br.com.cc.pessoas.filter.DocumentoFilter;
import br.com.cc.pessoas.repository.DocumentoRepository;
import br.com.cc.pessoas.repository.PessoaRepository;
import br.com.cc.pessoas.service.exceptions.DatabaseException;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentoService {

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @PersistenceContext
    private EntityManager manager;

    @Transactional(readOnly = true)
    public Page<DocumentoDTO> pesquisarComPaginacao(
            DocumentoFilter filter,
            Pageable pageable) {

        return documentoRepository
                .filtrar(filter, pageable)
                .map(DocumentoDTO::fromDocumento);
    }

    @Transactional(readOnly = true)
    public List<DocumentoDTO> pesquisarSemPaginacao(
            DocumentoFilter filter) {

        return documentoRepository
                .filtrar(filter)
                .stream()
                .map(DocumentoDTO::fromDocumento)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentoDTO findById(Long id) {

        Documento documento = buscarDocumento(id);

        return DocumentoDTO.fromDocumento(documento);
    }

    @Transactional(readOnly = true)
    public List<DocumentoDTO> findDocumentoByPessoaId(
            Long pessoaId) {

        validarExistenciaPessoa(pessoaId);

        return documentoRepository
                .findByPessoaId(pessoaId)
                .stream()
                .map(DocumentoDTO::fromDocumento)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<DocumentoDTO> findDocumentosByEstabelecimentoId(
            Long dadosPessoaJuridicaId) {

        DadosPessoaJuridica estabelecimento =
                manager.find(
                        DadosPessoaJuridica.class,
                        dadosPessoaJuridicaId
                );

        if (estabelecimento == null) {
            throw new ObjectNotFoundException(
                    "Estabelecimento com ID "
                            + dadosPessoaJuridicaId
                            + " não encontrado."
            );
        }

        return documentoRepository
                .findByDadosPessoaJuridicaId(
                        dadosPessoaJuridicaId
                )
                .stream()
                .map(DocumentoDTO::fromDocumento)
                .toList();
    }

    @Transactional
    public Documento insert(
            DocumentoCreateDTO dados) {

        Pessoa pessoa = buscarPessoa(dados.pessoaId());

        Documento documento = new Documento();

        documento.setPessoa(pessoa);

        documento.setDadosPessoaJuridica(
                buscarEstabelecimentoDaPessoa(
                        dados.dadosPessoaJuridicaId(),
                        pessoa
                )
        );

        preencherDadosDocumento(
                documento,
                dados
        );

        return documentoRepository.save(documento);
    }

    @Transactional
    public DocumentoDTO update(
            Long id,
            DocumentoUpdateDTO dados) {

        Documento documento =
                buscarDocumento(id);

        Pessoa pessoa =
                documento.getPessoa();

        documento.setDadosPessoaJuridica(
                buscarEstabelecimentoDaPessoa(
                        dados.dadosPessoaJuridicaId(),
                        pessoa
                )
        );

        preencherDadosDocumento(
                documento,
                dados
        );

        Documento documentoAtualizado =
                documentoRepository.saveAndFlush(
                        documento
                );

        return DocumentoDTO.fromDocumento(
                documentoAtualizado
        );
    }

    /*@Transactional
    public Documento update(
            Long id,
            DocumentoUpdateDTO dados) {

        Documento documento = buscarDocumento(id);

        Pessoa pessoa = documento.getPessoa();

        documento.setDadosPessoaJuridica(
                buscarEstabelecimentoDaPessoa(
                        dados.dadosPessoaJuridicaId(),
                        pessoa
                )
        );

        preencherDadosDocumento(
                documento,
                dados
        );

        return documentoRepository.save(documento);
    }*/

    @Transactional
    public void delete(Long id) {

        buscarDocumento(id);

        try {
            documentoRepository.deleteById(id);
            documentoRepository.flush();

        } catch (EmptyResultDataAccessException e) {
            throw new ObjectNotFoundException(
                    "Documento não encontrado. Id: " + id
            );

        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    private void preencherDadosDocumento(
            Documento documento,
            DocumentoCreateDTO dados) {

        documento.setTipoDocumento(
                TipoDocumento.toTipoDocumentoEnum(
                        dados.tipoDocumento()
                )
        );

        documento.setNumeroDocumento(
                dados.numeroDocumento()
        );

        documento.setDataDocumento(
                dados.dataDocumento()
        );

        documento.setDataExpedicao(
                dados.dataExpedicao()
        );

        documento.setDocumentoOrigem(
                dados.documentoOrigem()
        );

        documento.setOrgaoExpedidor(
                dados.orgaoExpedidor()
        );

        documento.setDataPrimeiraCnh(
                dados.dataPrimeiraCnh()
        );

        documento.setDataValidade(
                dados.dataValidade()
        );

        documento.setCategoriaCnh(
                dados.categoriaCnh()
        );

        documento.setZona(
                dados.zona()
        );

        documento.setSecao(
                dados.secao()
        );

        documento.setObservacao(
                dados.observacao()
        );
    }

    private void preencherDadosDocumento(
            Documento documento,
            DocumentoUpdateDTO dados) {

        documento.setTipoDocumento(
                TipoDocumento.toTipoDocumentoEnum(
                        dados.tipoDocumento()
                )
        );

        documento.setNumeroDocumento(
                dados.numeroDocumento()
        );

        documento.setDataDocumento(
                dados.dataDocumento()
        );

        documento.setDataExpedicao(
                dados.dataExpedicao()
        );

        documento.setDocumentoOrigem(
                dados.documentoOrigem()
        );

        documento.setOrgaoExpedidor(
                dados.orgaoExpedidor()
        );

        documento.setDataPrimeiraCnh(
                dados.dataPrimeiraCnh()
        );

        documento.setDataValidade(
                dados.dataValidade()
        );

        documento.setCategoriaCnh(
                dados.categoriaCnh()
        );

        documento.setZona(
                dados.zona()
        );

        documento.setSecao(
                dados.secao()
        );

        documento.setObservacao(
                dados.observacao()
        );
    }

    private Documento buscarDocumento(Long id) {

        return documentoRepository
                .findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException(
                                "Documento não encontrado. Id: " + id
                        )
                );
    }

    private Pessoa buscarPessoa(Long pessoaId) {

        return pessoaRepository
                .findById(pessoaId)
                .orElseThrow(() ->
                        new ObjectNotFoundException(
                                "Pessoa com ID "
                                        + pessoaId
                                        + " não encontrada."
                        )
                );
    }

    private void validarExistenciaPessoa(Long pessoaId) {

        if (!pessoaRepository.existsById(pessoaId)) {
            throw new ObjectNotFoundException(
                    "Pessoa com ID "
                            + pessoaId
                            + " não encontrada."
            );
        }
    }

    private DadosPessoaJuridica buscarEstabelecimentoDaPessoa(
            Long dadosPessoaJuridicaId,
            Pessoa pessoa) {

        if (dadosPessoaJuridicaId == null) {
            return null;
        }

        DadosPessoaJuridica estabelecimento =
                manager.find(
                        DadosPessoaJuridica.class,
                        dadosPessoaJuridicaId
                );

        if (estabelecimento == null) {
            throw new ObjectNotFoundException(
                    "Estabelecimento com ID "
                            + dadosPessoaJuridicaId
                            + " não encontrado."
            );
        }

        if (estabelecimento.getPessoa() == null
                || !estabelecimento
                .getPessoa()
                .getId()
                .equals(pessoa.getId())) {

            throw new IllegalArgumentException(
                    "O estabelecimento informado não pertence à pessoa."
            );
        }

        return estabelecimento;
    }
}