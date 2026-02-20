package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.pais.PaisCreateDTO;
import br.com.cc.pessoas.dto.pais.PaisDTO;
import br.com.cc.pessoas.dto.pais.PaisUpdateDTO;
import br.com.cc.pessoas.entity.Pais;
import br.com.cc.pessoas.filter.PaisFilter;
import br.com.cc.pessoas.repository.PaisRepository;
import br.com.cc.pessoas.service.PaisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import java.util.List;

@RestController
@RequestMapping("/paises")
@RequiredArgsConstructor
public class PaisController {

    @Autowired
    public PaisService paisService;
    @Autowired private PaisRepository paisRepository;
    @GetMapping("/list")
    public List<PaisDTO> listar(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String nome
    ) {
        PaisFilter filter = new PaisFilter();
        filter.setId(id);
        filter.setNome(nome);

        return paisService.listar(filter);
    }

    @GetMapping("/filter")
    public Page<PaisDTO> pesquisar(PaisFilter filter, Pageable pageable) {
        return paisService.pesquisar(filter, pageable);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity findById(@PathVariable Long id){
        Pais pais = paisService.findById(id);
        return ResponseEntity.ok().body(PaisDTO.fromPais(pais));
    }

    // Inserir
    @PostMapping
    @Transactional
    public ResponseEntity insert(@RequestBody @Valid PaisCreateDTO dados){
        var paisSalva = paisService.insert(dados);
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(paisSalva.getId())
                .toUri();
        return ResponseEntity.created(uri).body(PaisDTO.fromPais(paisSalva));
    }

    // ALTERAR
    @Transactional
    @PutMapping(value = "/{id}")
    public ResponseEntity update(@PathVariable @Valid Long id, @RequestBody PaisUpdateDTO dados){
        var salva = paisService.update(id, dados);
        return ResponseEntity.ok().body(PaisDTO.fromPais(salva));
    }

    // Deletar
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        paisService.delete(id);
    }
}