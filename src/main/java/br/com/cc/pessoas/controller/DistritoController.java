package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.distrito.DistritoCreateDTO;
import br.com.cc.pessoas.dto.distrito.DistritoDTO;
import br.com.cc.pessoas.dto.distrito.DistritoUpdateDTO;
import br.com.cc.pessoas.entity.Distrito;
import br.com.cc.pessoas.filter.DistritoFilter;

import br.com.cc.pessoas.service.DistritoService;
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
@RequestMapping("/distritos")
public class DistritoController {

    @Autowired private DistritoService distritoService;

    @GetMapping("/list")
    public List<DistritoDTO> listar(DistritoFilter filter) {
        return distritoService.listar(filter);
    }

    @GetMapping("/filter")
    public Page<DistritoDTO> pesquisar(DistritoFilter filter, Pageable pageable) {
        return distritoService.pesquisar(filter, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistritoDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(distritoService.findDtoById(id));
    }
    @Transactional
    @PostMapping
    public ResponseEntity<DistritoDTO> insert(@Valid @RequestBody DistritoCreateDTO dto) {

        Distrito distrito = distritoService.insert(dto);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(distrito.getId())
                .toUri();

        return ResponseEntity.created(uri).body(DistritoDTO.fromDistrito(distrito));
    }
    @Transactional
    @PutMapping("/{id}")
    public ResponseEntity<DistritoDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody DistritoUpdateDTO dto) {

        Distrito distrito = distritoService.update(id, dto);
        return ResponseEntity.ok(DistritoDTO.fromDistrito(distrito));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        distritoService.delete(id);
    }
}
