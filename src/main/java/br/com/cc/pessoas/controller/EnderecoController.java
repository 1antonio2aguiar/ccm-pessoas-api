package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.endereco.EnderecoCreateDTO;
import br.com.cc.pessoas.dto.endereco.EnderecoDTO;
import br.com.cc.pessoas.dto.endereco.EnderecoUpdateDTO;
import br.com.cc.pessoas.dto.logradouro.LogradouroDTO;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import com.pesoas.api.filter.enderecos.EnderecoFilter;
import br.com.cc.pessoas.service.EnderecoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/enderecos")
public class EnderecoController {

    @Autowired private EnderecoService enderecoService;

    // Listar de enderecos
    /*@GetMapping
    public Page<EnderecoDTO> findall(@PageableDefault(sort={"id"}) Pageable paginacao) {
        return enderecoService.findAllPaginated(paginacao);
    }*/

    // Lista de Ceps com filter
    @GetMapping("/list")
    public List<EnderecoDTO> pesquisar(EnderecoFilter filter) {
        return enderecoService.pesquisarSemPaginacao(filter);
    }

    // Lista de enderecos com Paginacao e filter
    @GetMapping("/filter")
    public Page<EnderecoDTO> pesquisar(EnderecoFilter filter, Pageable pageable) {
        return enderecoService.pesquisarComPaginacao(filter, pageable);
    }

    // cep por id
    @GetMapping(value = "/id/{id}")
    public ResponseEntity<EnderecoDTO> findById(@PathVariable Long id) {
        EnderecoDTO enderecoDto = enderecoService.findById(id);
        return ResponseEntity.ok().body(enderecoDto);
    }

    @GetMapping("/por-pessoa/{pessoaId}")
    public ResponseEntity<List<EnderecoDTO>> findEnderecosPorPessoa(@PathVariable Long pessoaId) {

        try {
            List<EnderecoDTO> enderecos = enderecoService.findEnderecosByPessoaId(pessoaId);
            return ResponseEntity.ok(enderecos); // Retorna 200 OK mesmo se a lista for vazia
        } catch (ObjectNotFoundException e) {
            // Se você tiver um @ControllerAdvice para ObjectNotFoundException, ele cuidará disso.
            // Caso contrário, você pode retornar 404 aqui:
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Ou uma mensagem de erro
        }
    }

    // Inserir
    @PostMapping
    @Transactional
    public ResponseEntity insert(@RequestBody @Valid EnderecoCreateDTO dados){
        var enderecoSalva = enderecoService.insert(dados);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/id")
                .buildAndExpand(enderecoSalva.getId()).toUri();
        return ResponseEntity.created(uri).body(EnderecoDTO.fromEndereco(enderecoSalva));
    }

    @PutMapping("/{enderecoId}/definir-como-principal")
    public ResponseEntity<Void> definirComoPrincipal(
            @PathVariable Long enderecoId,
            @RequestParam Long pessoaId) { // Ou obter pessoaId do usuário autenticado, se aplicável
        try {
            enderecoService.definirEnderecoComoPrincipal(pessoaId, enderecoId);
            return ResponseEntity.ok().build(); // Retorna 200 OK sem corpo se sucesso
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); // 400 se o endereço não pertence à pessoa
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404 se o endereço não for encontrado
        } catch (Exception e) {
            // Logar o erro
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ALTERAR
    @Transactional
    @PutMapping(value = "/{id}")
    public ResponseEntity update(@PathVariable @Valid Long id, @RequestBody EnderecoUpdateDTO dados){
        var salvar = enderecoService.update(id, dados);
        return ResponseEntity.ok(EnderecoDTO.fromEndereco(salvar));
    }

    // DELETAR
    @Transactional
    @DeleteMapping(value = "/{id}")
    public ResponseEntity delete(@PathVariable Long id){
        enderecoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
