package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.logradouro.*;
import br.com.cc.pessoas.filter.LogradouroFilter;
import br.com.cc.pessoas.service.LogradouroService;
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
@RequestMapping("/logradouros")
public class LogradouroController {

    @Autowired
    private LogradouroService logradouroService;

    @GetMapping("/list")
    public List<LogradouroDTO> listar(LogradouroFilter filter) {
        return logradouroService.listar(filter);
    }

    @GetMapping("/filter")
    public Page<LogradouroDTO> pesquisar(LogradouroFilter filter, Pageable pageable) {
        return logradouroService.pesquisar(filter, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogradouroDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(logradouroService.findDtoById(id));
    }

    @PostMapping
    @Transactional
    public ResponseEntity insert(@RequestBody @Valid LogradouroCreateDTO dto) {
        var salvo = logradouroService.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(salvo.getId())
                .toUri();
        return ResponseEntity.created(uri).body(LogradouroDTO.fromLogradouro(salvo));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity update(@PathVariable Long id,
                                 @RequestBody LogradouroUpdateDTO dto) {
        var salvo = logradouroService.update(id, dto);
        return ResponseEntity.ok(LogradouroDTO.fromLogradouro(salvo));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        logradouroService.delete(id);
    }
}
