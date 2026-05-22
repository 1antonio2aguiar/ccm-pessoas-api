package br.com.cc.pessoas.unificacao.dashboard.service;

import br.com.cc.pessoas.unificacao.dashboard.dto.CadUnicoDashboardResumoDTO;
import br.com.cc.pessoas.unificacao.dashboard.repository.CadUnicoDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CadUnicoDashboardService {

    private final CadUnicoDashboardRepository repository;

    public CadUnicoDashboardResumoDTO buscarResumo() {
        return repository.buscarResumo();
    }
}