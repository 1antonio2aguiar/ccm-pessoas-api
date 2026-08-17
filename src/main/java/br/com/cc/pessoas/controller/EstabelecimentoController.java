package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.estabelecimento.EstabelecimentoCreateDTO;
import br.com.cc.pessoas.dto.estabelecimento.EstabelecimentoDTO;
import br.com.cc.pessoas.dto.estabelecimento.EstabelecimentoUpdateDTO;
import br.com.cc.pessoas.filter.PessoaFilter;
import br.com.cc.pessoas.service.EstabelecimentoService;
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
@RequestMapping("/estabelecimentos")
public class EstabelecimentoController {

    @Autowired
    private EstabelecimentoService estabelecimentoService;

    @GetMapping("/list")
    public List<EstabelecimentoDTO> listar(
            PessoaFilter filter) {

        return estabelecimentoService.listar(filter);
    }

    @GetMapping("/filter")
    public Page<EstabelecimentoDTO> pesquisar(
            PessoaFilter filter,
            Pageable pageable) {

        return estabelecimentoService.pesquisar(
                filter,
                pageable
        );
    }
    @GetMapping("/{id}")
    public ResponseEntity<EstabelecimentoDTO> findById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                estabelecimentoService.findById(id)
        );
    }
    @PostMapping
    public ResponseEntity<EstabelecimentoDTO> insert(
            @RequestBody @Valid EstabelecimentoCreateDTO dto) {

        EstabelecimentoDTO estabelecimentoSalvo =
                estabelecimentoService.insert(dto);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(estabelecimentoSalvo.id())
                .toUri();

        return ResponseEntity
                .created(uri)
                .body(estabelecimentoSalvo);
    }
    @PutMapping("/{id}")
    public ResponseEntity<EstabelecimentoDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid EstabelecimentoUpdateDTO dto) {

        return ResponseEntity.ok(
                estabelecimentoService.update(id, dto)
        );
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        estabelecimentoService.delete(id);

        return ResponseEntity.noContent().build();
    }
}