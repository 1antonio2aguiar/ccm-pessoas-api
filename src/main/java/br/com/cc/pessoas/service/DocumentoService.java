package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.documento.DocumentoCreateDTO;
import br.com.cc.pessoas.dto.documento.DocumentoDTO;
import br.com.cc.pessoas.dto.documento.DocumentoUpdateDTO;
import br.com.cc.pessoas.entity.Documento;
import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.entity.enuns.TipoDocumento;
import br.com.cc.pessoas.filter.DocumentoFilter;
import br.com.cc.pessoas.repository.DocumentoRepository;
import br.com.cc.pessoas.repository.PessoaRepository;
import br.com.cc.pessoas.service.exceptions.DatabaseException;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentoService {
    @Autowired
    private DocumentoRepository documentoRepository;
    @Autowired private PessoaRepository pessoaRepository;

    public Page<DocumentoDTO> pesquisarComPaginacao(DocumentoFilter filter, Pageable pageable) {
        Page<Documento> page = documentoRepository.filtrar(filter, pageable);
        return page.map(DocumentoDTO::fromDocumento);
    }

    public List<DocumentoDTO> pesquisarSemPaginacao(DocumentoFilter filter) {
        return documentoRepository.filtrar(filter)
                .stream()
                .map(DocumentoDTO::fromDocumento)
                .toList();
    }
    @Transactional(readOnly = true)
    public DocumentoDTO findById(Long id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Documento não encontrado. Id: " + id));
        return DocumentoDTO.fromDocumento(documento);
    }

    @Transactional(readOnly = true)
    public List<DocumentoDTO> findDocumentoByPessoaId(Long pessoaId) {
        if (!pessoaRepository.existsById(pessoaId)) {
            throw new ObjectNotFoundException("Pessoa com ID " + pessoaId + " não encontrada.");
        }
        // Usa o método corrigido do contatoRepository
        List<Documento> documentos = documentoRepository.findByPessoaId(pessoaId);
        return documentos.stream()
            .map(DocumentoDTO::fromDocumento)
            .collect(Collectors.toList());
    }

    //Insert
    public Documento insert(DocumentoCreateDTO dados){
        Documento documento = new Documento();
        BeanUtils.copyProperties(dados, documento, "id");

        //Busco a pessoa
        Pessoa pessoa = pessoaRepository.findById(dados.pessoaId())
                .orElseThrow(() -> new ObjectNotFoundException("Pessoa com ID " + dados.pessoaId() + " não encontrada."));
        documento.setPessoa(pessoa);

        // set o enum
        documento.setTipoDocumento(TipoDocumento.toTipoDocumentoEnum(dados.tipoDocumento()));

        Documento documentoInsert = documentoRepository.save(documento);
        return documentoInsert;
    }

    // update
    public Documento update(Long id, DocumentoUpdateDTO dados){

        Documento documentoUpd = documentoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Documento não cadastrado. Id: " + id));
        BeanUtils.copyProperties(dados, documentoUpd, "id");

        return documentoRepository.save(documentoUpd);
    }

    // Delete
    public void delete(Long id){
        Documento documentoDel = documentoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Documento não cadastrado. Id: " + id));
        try {
            documentoRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ObjectNotFoundException(id);
        } catch (DataIntegrityViolationException e){
            throw new DatabaseException(e.getMessage());
        }
    }

}