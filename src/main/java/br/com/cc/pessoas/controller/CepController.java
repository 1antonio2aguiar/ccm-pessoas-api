package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.cep.*;
import br.com.cc.pessoas.filter.CepFilter;
import br.com.cc.pessoas.service.CepService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/ceps")
public class CepController {

    @Autowired
    private CepService cepService;

    @GetMapping("/list")
    public List<CepDTO> listar(CepFilter filter) {
        return cepService.listar(filter);
    }

    @GetMapping("/filter")
    public Page<CepDTO> pesquisar(CepFilter filter, Pageable pageable) {
        return cepService.pesquisar(filter, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CepDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(cepService.findDtoById(id));
    }

    @PostMapping
    @Transactional
    public ResponseEntity insert(@RequestBody @Valid CepCreateDTO dto) {
        var salvo = cepService.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(salvo.getId())
                .toUri();
        return ResponseEntity.created(uri).body(CepDTO.fromCep(salvo));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity update(@PathVariable Long id,
                                 @RequestBody CepUpdateDTO dto) {
        var salvo = cepService.update(id, dto);
        return ResponseEntity.ok(CepDTO.fromCep(salvo));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cepService.delete(id);
    }
}