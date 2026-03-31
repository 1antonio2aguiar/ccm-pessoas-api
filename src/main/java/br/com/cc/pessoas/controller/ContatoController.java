package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.contato.ContatoCreateDTO;
import br.com.cc.pessoas.dto.contato.ContatoDTO;
import br.com.cc.pessoas.dto.contato.ContatoUpdateDTO;
import br.com.cc.pessoas.filter.ContatoFilter;
import br.com.cc.pessoas.service.ContatoService;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
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
@RequestMapping("/contatos")
public class ContatoController {

    @Autowired private ContatoService contatoService;

    // Lista de contatos com filter
    @GetMapping("/list")
    public List<ContatoDTO> pesquisar(ContatoFilter filter) {
        return contatoService.pesquisarSemPaginacao(filter);
    }

    // Lista de contatos com Paginacao e filter
    @GetMapping("/filter")
    public Page<ContatoDTO> pesquisar(ContatoFilter filter, Pageable pageable) {
        return contatoService.pesquisarComPaginacao(filter, pageable);
    }

    // contato por id
    @GetMapping(value = "/id/{id}")
    public ResponseEntity<ContatoDTO> findById(@PathVariable Long id) {
        ContatoDTO contatoDTO = contatoService.findById(id);
        return ResponseEntity.ok().body(contatoDTO);
    }

    @GetMapping("/por-pessoa/{pessoaId}")
    public ResponseEntity<List<ContatoDTO>> findContatoPorPessoa(@PathVariable Long pessoaId) {

        try {
            List<ContatoDTO> contatos = contatoService.findContatoByPessoaId(pessoaId);
            return ResponseEntity.ok(contatos); // Retorna 200 OK mesmo se a lista for vazia
        } catch (ObjectNotFoundException e) {
            // Se você tiver um @ControllerAdvice para ObjectNotFoundException, ele cuidará disso.
            // Caso contrário, você pode retornar 404 aqui:
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Ou uma mensagem de erro
        }
    }

    // Inserir
    @PostMapping
    @Transactional
    public ResponseEntity insert(@RequestBody @Valid ContatoCreateDTO dados){
        var contatoSalva = contatoService.insert(dados);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/id")
                .buildAndExpand(contatoSalva.getId()).toUri();
        return ResponseEntity.created(uri).body(ContatoDTO.fromContato(contatoSalva));
    }

    @PutMapping("/{contatoId}/definir-como-principal")
    public ResponseEntity<Void> definirComoPrincipal(
            @PathVariable Long contatoId,
            @RequestParam Long pessoaId) { // Ou obter pessoaId do usuário autenticado, se aplicável
        try {
            contatoService.definirContatoComoPrincipal(pessoaId, contatoId);
            return ResponseEntity.ok().build(); // Retorna 200 OK sem corpo se sucesso
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); // 400 se o contato não pertence à pessoa
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404 se o contato não for encontrado
        } catch (Exception e) {
            // Logar o erro
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ALTERAR
    @Transactional
    @PutMapping(value = "/{id}")
    public ResponseEntity update(@PathVariable @Valid Long id, @RequestBody ContatoUpdateDTO dados){
        var salvar = contatoService.update(id, dados);
        return ResponseEntity.ok(ContatoDTO.fromContato(salvar));
    }

    // DELETAR
    @Transactional
    @DeleteMapping(value = "/{id}")
    public ResponseEntity delete(@PathVariable Long id){
        contatoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
