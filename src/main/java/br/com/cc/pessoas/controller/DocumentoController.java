package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.contato.ContatoCreateDTO;
import br.com.cc.pessoas.dto.contato.ContatoDTO;
import br.com.cc.pessoas.dto.contato.ContatoUpdateDTO;
import br.com.cc.pessoas.dto.documento.DocumentoCreateDTO;
import br.com.cc.pessoas.dto.documento.DocumentoDTO;
import br.com.cc.pessoas.dto.documento.DocumentoUpdateDTO;
import br.com.cc.pessoas.filter.ContatoFilter;
import br.com.cc.pessoas.filter.DocumentoFilter;
import br.com.cc.pessoas.service.ContatoService;
import br.com.cc.pessoas.service.DocumentoService;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/documentos")
public class DocumentoController {

    @Autowired private DocumentoService documentoService;

    // Lista de documentos com filter
    @GetMapping("/list")
    public List<DocumentoDTO> pesquisar(DocumentoFilter filter) {
        return documentoService.pesquisarSemPaginacao(filter);
    }

    // Lista de documentos com Paginacao e filter
    @GetMapping("/filter")
    public Page<DocumentoDTO> pesquisar(DocumentoFilter filter, Pageable pageable) {
        return documentoService.pesquisarComPaginacao(filter, pageable);
    }

    // documento por id
    @GetMapping(value = "/id/{id}")
    public ResponseEntity<DocumentoDTO> findById(@PathVariable Long id) {
        DocumentoDTO documentoDTO = documentoService.findById(id);
        return ResponseEntity.ok().body(documentoDTO);
    }

    @GetMapping("/por-pessoa/{pessoaId}")
    public ResponseEntity<List<DocumentoDTO>> findDocumentoPorPessoa(@PathVariable Long pessoaId) {

        try {
            List<DocumentoDTO> documentos = documentoService.findDocumentoByPessoaId(pessoaId);
            return ResponseEntity.ok(documentos); // Retorna 200 OK mesmo se a lista for vazia
        } catch (ObjectNotFoundException e) {
            // Se você tiver um @ControllerAdvice para ObjectNotFoundException, ele cuidará disso.
            // Caso contrário, você pode retornar 404 aqui:
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Ou uma mensagem de erro
        }
    }

    // Inserir
    @PostMapping
    @Transactional
    public ResponseEntity insert(@RequestBody @Valid DocumentoCreateDTO dados){
        var documentoSalva = documentoService.insert(dados);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/id")
                .buildAndExpand(documentoSalva.getId()).toUri();
        return ResponseEntity.created(uri).body(DocumentoDTO.fromDocumento(documentoSalva));
    }

    // ALTERAR
    @Transactional
    @PutMapping(value = "/{id}")
    public ResponseEntity update(@PathVariable @Valid Long id, @RequestBody DocumentoUpdateDTO dados){
        var salvar = documentoService.update(id, dados);
        return ResponseEntity.ok(DocumentoDTO.fromDocumento(salvar));
    }

    // DELETAR
    @Transactional
    @DeleteMapping(value = "/{id}")
    public ResponseEntity delete(@PathVariable Long id){
        documentoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
