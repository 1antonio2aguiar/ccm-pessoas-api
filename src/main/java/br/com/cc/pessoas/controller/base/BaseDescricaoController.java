package br.com.cc.pessoas.controller.base;

import br.com.cc.pessoas.dto.base.DescricaoDTO;
import br.com.cc.pessoas.service.base.BaseDescricaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public abstract class BaseDescricaoController<T> {

    protected final BaseDescricaoService<T> service;

    protected BaseDescricaoController(BaseDescricaoService<T> service) {
        this.service = service;
    }

    @GetMapping
    public List<DescricaoDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DescricaoDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity insert(@RequestBody @Valid DescricaoDTO dto) {
        return ResponseEntity.ok(service.insert(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity update(@PathVariable Long id,
                                 @RequestBody @Valid DescricaoDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
