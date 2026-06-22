package br.com.cc.pessoas.unificacao.saneamento.saneController;

import br.com.cc.pessoas.unificacao.saneamento.saneDto.SanePessoaDTO;
import br.com.cc.pessoas.unificacao.saneamento.saneFilter.SanePessoaFilter;
import br.com.cc.pessoas.unificacao.saneamento.saneService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sane-pessoas")
@RequiredArgsConstructor
public class SanePessoaController {

    private final SanePessoaService service;
    private final SaneCargaPessoaCpfUnicoService saneCargaPessoaCpfUnicoService;
    private final SaneCargaPessoaJaExisteCadUnicoService saneCargaPessoaJaExisteCadUnicoService;
    private final SaneCargaPessoaCnpjUnicoService saneCargaPessoaCnpjUnicoService;
    private final SaneCargaPessoaCnpjJaExisteCadUnicoService saneCargaPessoaCnpjJaExisteCadUnicoService;
    @GetMapping("/cpf-unico")
    public Page<SanePessoaDTO> filtrarCpfUnico(SanePessoaFilter filter, Pageable pageable) {
        return service.filtrarListaSaneCpfUnico(filter, pageable);
    }
    @GetMapping("/cnpj-unico")
    public Page<SanePessoaDTO> filtrarCnpjUnico(SanePessoaFilter filter, Pageable pageable) {
        return service.filtrarListaSaneCnpjUnico(filter, pageable);
    }
    @PostMapping("/processar-cpf-unico/{pessoaId}")
    public String processarCpfUnico(@PathVariable Long pessoaId) {
        return saneCargaPessoaCpfUnicoService.processarPessoaUnica(pessoaId);
    }

    @PostMapping("/processar-ja-existe-cad-unico/{pessoaId}")
    public String processarJaExisteCadUnico(@PathVariable Long pessoaId) {
        return saneCargaPessoaJaExisteCadUnicoService.processarJaExisteCadUnico(pessoaId);
    }
    @PostMapping("/processar-cnpj-unico/{pessoaId}")
    public String processarCnpjUnico(@PathVariable Long pessoaId) {
        return saneCargaPessoaCnpjUnicoService.processarCnpjUnico(pessoaId);
    }
    @PostMapping("/processar-cnpj-ja-existe-cad-unico/{pessoaId}")
    public String processarCnpjJaExisteCadUnico(@PathVariable Long pessoaId) {
        return saneCargaPessoaCnpjJaExisteCadUnicoService.processarCnpjJaExisteCadUnico(pessoaId);
    }
}