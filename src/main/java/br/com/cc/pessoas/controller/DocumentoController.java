package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.documento.DocumentoCreateDTO;
import br.com.cc.pessoas.dto.documento.DocumentoDTO;
import br.com.cc.pessoas.dto.documento.DocumentoUpdateDTO;
import br.com.cc.pessoas.filter.DocumentoFilter;
import br.com.cc.pessoas.service.DocumentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/documentos")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    @GetMapping("/list")
    public ResponseEntity<List<DocumentoDTO>> pesquisarSemPaginacao(
            DocumentoFilter filter) {

        return ResponseEntity.ok(
                documentoService.pesquisarSemPaginacao(filter)
        );
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<DocumentoDTO>> pesquisarComPaginacao(
            DocumentoFilter filter,
            Pageable pageable) {

        return ResponseEntity.ok(
                documentoService.pesquisarComPaginacao(
                        filter,
                        pageable
                )
        );
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<DocumentoDTO> findById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                documentoService.findById(id)
        );
    }

    @GetMapping("/por-pessoa/{pessoaId}")
    public ResponseEntity<List<DocumentoDTO>> findDocumentoPorPessoa(
            @PathVariable Long pessoaId) {

        return ResponseEntity.ok(
                documentoService.findDocumentoByPessoaId(
                        pessoaId
                )
        );
    }
    @GetMapping("/por-estabelecimento/{dadosPessoaJuridicaId}")
    public ResponseEntity<List<DocumentoDTO>>
    findDocumentosPorEstabelecimento(
            @PathVariable Long dadosPessoaJuridicaId) {

        return ResponseEntity.ok(
                documentoService
                        .findDocumentosByEstabelecimentoId(
                                dadosPessoaJuridicaId
                        )
        );
    }
    @PostMapping
    public ResponseEntity<DocumentoDTO> insert(
            @RequestBody @Valid DocumentoCreateDTO dados) {

        var documentoSalvo =
                documentoService.insert(dados);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/id/{id}")
                .buildAndExpand(documentoSalvo.getId())
                .toUri();

        return ResponseEntity
                .created(uri)
                .body(
                        DocumentoDTO.fromDocumento(
                                documentoSalvo
                        )
                );
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentoDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid DocumentoUpdateDTO dados) {

        DocumentoDTO documentoAtualizado =
                documentoService.update(
                        id,
                        dados
                );

        return ResponseEntity.ok(
                documentoAtualizado
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        documentoService.delete(id);

        return ResponseEntity.noContent().build();
    }
}