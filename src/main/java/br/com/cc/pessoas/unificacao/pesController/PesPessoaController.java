package br.com.cc.pessoas.unificacao.pesController;

import br.com.cc.pessoas.unificacao.pesDto.PesPessoaDTO;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesFilter.PesPessoaFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesPessoaRepository;
import br.com.cc.pessoas.unificacao.pesService.PesPessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pes-pessoas")
@RequiredArgsConstructor
public class PesPessoaController {

    @Autowired
    private PesPessoaService service;
    @Autowired
    private PesPessoaRepository pesPessoaRepository;

    @GetMapping
    public Page<PesPessoaDTO> pesquisar(@ModelAttribute PesPessoaFilter filter, Pageable pageable) {
        return service.filtrar(filter, pageable);
    }

    @GetMapping("/list")
    public List<PesPessoaDTO> listar(@ModelAttribute PesPessoaFilter filter) {
        return service.filtrar(filter);
    }

    @GetMapping("/cpf-unico-nao-migradas")
    public Page<PesPessoaDTO> filtrarCpfUnicoNaoMigradas(PesPessoaFilter filter, Pageable pageable) {
        return pesPessoaRepository.filtrarCpfUnicoNaoMigradas(filter, pageable);
    }

    @GetMapping("/cnpj-unico-nao-migradas")
    public Page<PesPessoaDTO> filtrarCnpjUnicoNaoMigradas(@ModelAttribute PesPessoaFilter filter, Pageable pageable) {
        return pesPessoaRepository.filtrarCnpjUnicoNaoMigradas(filter, pageable);
    }

    @GetMapping("/cpf-duplicado-nao-migradas")
    public Page<PesPessoaDTO> filtrarCpfDuplicadoNaoMigradas(@ModelAttribute PesPessoaFilter filter, Pageable pageable) {
        return pesPessoaRepository.filtrarCpfDuplicadoNaoMigradas(filter, pageable);
    }

    @GetMapping("/cnpj-duplicado-nao-migradas")
    public Page<PesPessoaDTO> filtrarCnpjDuplicadoNaoMigradas(@ModelAttribute PesPessoaFilter filter, Pageable pageable) {
        return pesPessoaRepository.filtrarCnpjDuplicadoNaoMigradas(filter, pageable);
    }
}