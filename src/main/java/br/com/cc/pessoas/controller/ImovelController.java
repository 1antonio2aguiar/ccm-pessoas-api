package br.com.cc.pessoas.controller;

import br.com.cc.pessoas.dto.imovel.ImovelDTO;
import br.com.cc.pessoas.service.ImovelService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/imoveis")
public class ImovelController {
    private final ImovelService imovelService;

    public ImovelController(ImovelService imovelService) {
        this.imovelService = imovelService;
    }

    @GetMapping("/pessoa/{pessoasCdUnico}")
    public ResponseEntity<List<ImovelDTO>> listarPorPessoa(
            @PathVariable Long pessoasCdUnico,
            @RequestParam(defaultValue = "1992") Integer anoInicial,
            @RequestParam(defaultValue = "2026") Integer anoFinal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCorrecao) {
        LocalDate data = dataCorrecao == null ? LocalDate.now() : dataCorrecao;
        return ResponseEntity.ok(imovelService.listarPorPessoa(pessoasCdUnico, anoInicial, anoFinal, data));
    }
}
