package br.com.cc.pessoas.unificacao.dashboard.controller;

import br.com.cc.pessoas.unificacao.dashboard.dto.CadUnicoDashboardResumoDTO;
import br.com.cc.pessoas.unificacao.dashboard.service.CadUnicoDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cad-unico-dashboard")
@RequiredArgsConstructor
public class CadUnicoDashboardController {

    private final CadUnicoDashboardService service;

    @GetMapping("/resumo")
    public CadUnicoDashboardResumoDTO buscarResumo() {
        return service.buscarResumo();
    }
}