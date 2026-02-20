package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.bairro.BairroCreateDTO;
import br.com.cc.pessoas.dto.bairro.BairroDTO;
import br.com.cc.pessoas.dto.bairro.BairroUpdateDTO;
import br.com.cc.pessoas.filter.BairroFilter;
import br.com.cc.pessoas.service.BairroService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/bairros")
public class BairroController {

    @Autowired
    private BairroService bairroService;

    @GetMapping("/list")
    public List<BairroDTO> listar(BairroFilter filter) {
        return bairroService.listar(filter);
    }

    @GetMapping("/filter")
    public Page<BairroDTO> pesquisar(BairroFilter filter, Pageable pageable) {
        return bairroService.pesquisar(filter, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BairroDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bairroService.findDtoById(id));
    }

    // INSERT
    @PostMapping
    @Transactional
    public ResponseEntity insert(@RequestBody @Valid BairroCreateDTO dto){
        var bairroSalva = bairroService.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/id")
                .buildAndExpand(bairroSalva.getId()).toUri();
        return ResponseEntity.created(uri).body(BairroDTO.fromBairro(bairroSalva));
    }

    // UPDATE
    @Transactional
    @PutMapping(value = "/{id}")
    public ResponseEntity update(@PathVariable @Valid Long id, @RequestBody BairroUpdateDTO dto){
        var salva = bairroService.update(id, dto);
        return ResponseEntity.ok().body(BairroDTO.fromBairro(salva));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bairroService.delete(id);
    }
}

