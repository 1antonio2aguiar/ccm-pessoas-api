package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.tipoLogradouro.TipoLogradouroCreateDTO;
import br.com.cc.pessoas.dto.tipoLogradouro.TipoLogradouroDTO;
import br.com.cc.pessoas.dto.tipoLogradouro.TipoLogradouroUpdateDTO;
import br.com.cc.pessoas.filter.TipoLogradouroFilter;
import br.com.cc.pessoas.repository.TipoLogradouroRepository;
import br.com.cc.pessoas.service.TipoLogradouroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/tipoLogradouro")
@RequiredArgsConstructor
public class TipoLogradouroController {

    @Autowired
    public TipoLogradouroService tlService;
    @Autowired private TipoLogradouroRepository tlRepository;
    @GetMapping("/list")
    public List<TipoLogradouroDTO> listar(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String nome
    ) {
        TipoLogradouroFilter filter = new TipoLogradouroFilter();
        filter.setId(id);
        filter.setDescricao(filter.getDescricao());

        return tlService.listar(filter);
    }

    @GetMapping("/filter")
    public Page<TipoLogradouroDTO> pesquisar(TipoLogradouroFilter filter, Pageable pageable) {
        return tlService.pesquisar(filter, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoLogradouroDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(tlService.findDtoById(id));
    }

    // Inserir
    @PostMapping
    @Transactional
    public ResponseEntity insert(@RequestBody @Valid TipoLogradouroCreateDTO dados){
        var tlSalva = tlService.insert(dados);
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(tlSalva.getId())
                .toUri();
        return ResponseEntity.created(uri).body(TipoLogradouroDTO.fromEntity(tlSalva));
    }

    // ALTERAR
    @Transactional
    @PutMapping(value = "/{id}")
    public ResponseEntity update(@PathVariable @Valid Long id, @RequestBody TipoLogradouroUpdateDTO dados){
        var salva = tlService.update(id, dados);
        return ResponseEntity.ok().body(TipoLogradouroDTO.fromEntity(salva));
    }

    // Deletar
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tlService.delete(id);
    }
}