package br.com.cc.pessoas.unificacao.rh.rhController;

import br.com.cc.pessoas.unificacao.rh.rhDto.RhPessoaDTO;
import br.com.cc.pessoas.unificacao.rh.rhFilter.RhPessoaFilter;
import br.com.cc.pessoas.unificacao.rh.rhService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rh-pessoas")
@RequiredArgsConstructor
public class RhPessoaController {

    private final RhPessoaService service;
    private final RhCargaPessoaCpfUnicoService rhCargaPessoaCpfUnicoService;
    private final RhCargaPessoaCpfDuplicadoService rhCargaPessoaCpfDuplicadoService;
    private final RhCargaPessoaJaExisteCadUnicoService rhCargaPessoaJaExisteCadUnicoService;

    @GetMapping
    public Page<RhPessoaDTO> filtrar(@ModelAttribute RhPessoaFilter filter, Pageable pageable) {
        return service.filtrarListaRh(filter, pageable);
    }

    @PostMapping("/processar-cpf-unico/{pessoaId}")
    public String processarCpfUnico(@PathVariable Long pessoaId) {
        rhCargaPessoaCpfUnicoService.processarPessoaUnica(pessoaId);
        return "Pessoa física única do RH processada com sucesso.";
    }

    @PostMapping("/processar-cpf-duplicado/{pessoaId}")
    public String processarCpfDuplicado(@PathVariable Long pessoaId) {
        rhCargaPessoaCpfDuplicadoService.processarPessoaUnica(pessoaId);
        return "Grupo de CPF duplicado do RH processado com sucesso.";
    }

    @PostMapping("/processar-ja-existe-cad-unico/{pessoaId}")
    public String processarJaExisteCadUnico(@PathVariable Long pessoaId) {
        rhCargaPessoaJaExisteCadUnicoService.processarPessoaUnica(pessoaId);
        return "Pessoa RH vinculada ao Cadastro Único existente com sucesso.";
    }

    private final RhCargaPessoaCnpjUnicoService rhCargaPessoaCnpjUnicoService;

    @PostMapping("/processar-cnpj-unico/{pessoaId}")
    public String processarCnpjUnico(@PathVariable Long pessoaId) {
        rhCargaPessoaCnpjUnicoService.processarPessoaUnica(pessoaId);
        return "Pessoa jurídica única do RH processada com sucesso.";
    }
}