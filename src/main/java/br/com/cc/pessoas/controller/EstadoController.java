package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.estado.EstadoCreateDTO;
import br.com.cc.pessoas.dto.estado.EstadoDTO;
import br.com.cc.pessoas.dto.estado.EstadoUpdateDTO;
import br.com.cc.pessoas.entity.Estado;
import br.com.cc.pessoas.filter.EstadoFilter;
import br.com.cc.pessoas.service.EstadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/estados")
public class EstadoController {

    private final EstadoService estadoService;

    public EstadoController(EstadoService estadoService) {
        this.estadoService = estadoService;
    }

    @GetMapping("/list")
    public List<EstadoDTO> listar(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long paisId,
            @RequestParam(required = false) String nome
    ) {
        EstadoFilter filter = new EstadoFilter();
        filter.setId(id);
        filter.setPaisId(paisId);
        filter.setNome(nome);

        return estadoService.listar(filter);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstadoDTO> findById(@PathVariable Long id) {
        Estado estado = estadoService.findById(id);
        return ResponseEntity.ok(EstadoDTO.fromEstado(estado));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<EstadoDTO> insert(
            @RequestBody @Valid EstadoCreateDTO dto) {

        Estado estado = estadoService.insert(dto);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(estado.getId())
                .toUri();

        return ResponseEntity.created(uri).body(EstadoDTO.fromEstado(estado));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<EstadoDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid EstadoUpdateDTO dto) {

        Estado estado = estadoService.update(id, dto);
        return ResponseEntity.ok(EstadoDTO.fromEstado(estado));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        estadoService.delete(id);
    }
}
