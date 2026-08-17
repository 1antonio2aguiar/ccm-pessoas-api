package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.endereco.EnderecoCreateDTO;
import br.com.cc.pessoas.dto.endereco.EnderecoDTO;
import br.com.cc.pessoas.dto.endereco.EnderecoUpdateDTO;
import br.com.cc.pessoas.entity.Endereco;
import br.com.cc.pessoas.service.EnderecoService;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import com.pesoas.api.filter.enderecos.EnderecoFilter;
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
@RequestMapping("/enderecos")
public class EnderecoController {

    @Autowired
    private EnderecoService enderecoService;

    // =========================================================
    // PESQUISA SEM PAGINAÇÃO
    // =========================================================

    @GetMapping("/list")
    public List<EnderecoDTO> pesquisar(EnderecoFilter filter) {
        return enderecoService.pesquisarSemPaginacao(filter);
    }

    // =========================================================
    // PESQUISA COM PAGINAÇÃO
    // =========================================================

    @GetMapping("/filter")
    public Page<EnderecoDTO> pesquisar(
            EnderecoFilter filter,
            Pageable pageable) {

        return enderecoService.pesquisarComPaginacao(
                filter,
                pageable
        );
    }

    // =========================================================
    // BUSCAR POR ID
    // =========================================================

    @GetMapping("/id/{id}")
    public ResponseEntity<EnderecoDTO> findById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                enderecoService.findById(id)
        );
    }

    // =========================================================
    // ENDEREÇOS DA PESSOA
    //
    // Retorna:
    // - endereços do CPF;
    // - endereços dos estabelecimentos/CNPJs.
    //
    // O campo dadosPessoaJuridicaId do DTO identifica o titular.
    // =========================================================

    @GetMapping("/por-pessoa/{pessoaId}")
    public ResponseEntity<List<EnderecoDTO>> findEnderecosPorPessoa(
            @PathVariable Long pessoaId) {

        return ResponseEntity.ok(
                enderecoService.findEnderecosByPessoaId(pessoaId)
        );
    }
    @GetMapping("/por-estabelecimento/{dadosPessoaJuridicaId}")
    public ResponseEntity<List<EnderecoDTO>>
    findEnderecosPorEstabelecimento(
            @PathVariable Long dadosPessoaJuridicaId) {

        return ResponseEntity.ok(
                enderecoService
                        .findEnderecosByEstabelecimentoId(
                                dadosPessoaJuridicaId
                        )
        );
    }

    // =========================================================
    // INSERT
    // =========================================================

    @PostMapping
    public ResponseEntity<EnderecoDTO> insert(
            @RequestBody @Valid EnderecoCreateDTO dados) {

        EnderecoDTO enderecoSalvo =
                enderecoService.insert(dados);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/id/{id}")
                .buildAndExpand(enderecoSalvo.id())
                .toUri();

        return ResponseEntity
                .created(uri)
                .body(enderecoSalvo);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<EnderecoDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid EnderecoUpdateDTO dados) {

        return ResponseEntity.ok(
                enderecoService.update(id, dados)
        );
    }

    // =========================================================
    // DEFINIR COMO PRINCIPAL
    //
    // O service identifica automaticamente se o endereço é:
    // - principal do CPF;
    // - principal de um CNPJ específico.
    // =========================================================

    @PutMapping("/{enderecoId}/definir-como-principal")
    public ResponseEntity<Void> definirComoPrincipal(
            @PathVariable Long enderecoId,
            @RequestParam Long pessoaId) {

        try {
            enderecoService.definirEnderecoComoPrincipal(
                    pessoaId,
                    enderecoId
            );

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();

        } catch (ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        enderecoService.delete(id);

        return ResponseEntity.noContent().build();
    }
}