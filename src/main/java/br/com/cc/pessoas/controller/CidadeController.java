package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.cidade.CidadeCreateDTO;
import br.com.cc.pessoas.dto.cidade.CidadeDTO;
import br.com.cc.pessoas.dto.cidade.CidadeUpdateDTO;
import br.com.cc.pessoas.entity.Cidade;
import br.com.cc.pessoas.filter.CidadeFilter;
import br.com.cc.pessoas.repository.CidadeRepository;
import br.com.cc.pessoas.service.CidadeService;

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
@RequestMapping("/cidades")
public class CidadeController {

    @Autowired CidadeService cidadeService;
    @Autowired CidadeRepository cidadeRepository;

    public CidadeController(CidadeService cidadeService) {
        this.cidadeService = cidadeService;
    }

    @GetMapping("/list")
    public List<CidadeDTO> pesquisar(CidadeFilter filter ) {
        return cidadeService.listar(filter);
    }

    @GetMapping("/filter")
    public Page<CidadeDTO> pesquisar(CidadeFilter filter, Pageable pageable) {
        return cidadeService.pesquisar(filter, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CidadeDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(cidadeService.findDtoById(id));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CidadeDTO> insert(
            @RequestBody @Valid CidadeCreateDTO dto) {

        Cidade cidade = cidadeService.insert(dto);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(cidade.getId())
                .toUri();

        return ResponseEntity.created(uri).body(CidadeDTO.fromCidade(cidade));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<CidadeDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid CidadeUpdateDTO dto) {

        Cidade cidade = cidadeService.update(id, dto);
        return ResponseEntity.ok(CidadeDTO.fromCidade(cidade));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cidadeService.delete(id);
    }
}
