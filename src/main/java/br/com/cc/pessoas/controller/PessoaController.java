package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.pessoa.PessoaCreateDTO;
import br.com.cc.pessoas.dto.pessoa.PessoaDTO;
import br.com.cc.pessoas.dto.pessoa.PessoaUpdateDTO;
import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.filter.PessoaFilter;
import br.com.cc.pessoas.service.PessoaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/pessoas")
public class PessoaController {

    @Autowired
    private PessoaService pessoaService;

    @GetMapping("/list")
    public List<PessoaDTO> listar(PessoaFilter filter) {
        return pessoaService.listar(filter);
    }

    @GetMapping("/filter")
    public Page<PessoaDTO> pesquisar(PessoaFilter filter, Pageable pageable) {
        return pessoaService.pesquisar(filter, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PessoaDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(pessoaService.findDtoById(id));
    }

    @PostMapping
    public ResponseEntity<PessoaDTO> insert(@RequestBody @Valid PessoaCreateDTO dto) {

        Pessoa pessoaSalva = pessoaService.insert(dto);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(pessoaSalva.getId())
                .toUri();

        return ResponseEntity
                .created(uri)
                .body(PessoaDTO.fromPessoa(pessoaSalva));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PessoaDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid PessoaUpdateDTO dto) {

        PessoaDTO salva = pessoaService.update(id, dto);

        return ResponseEntity
                .ok()
                .body(salva);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        pessoaService.delete(id);
    }
}
