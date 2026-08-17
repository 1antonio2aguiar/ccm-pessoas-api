package br.com.cc.pessoas.unificacao.controle.ctrlController;
import br.com.cc.pessoas.unificacao.controle.ctrlService.ControleMigracaoPessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/controle-migracao-pessoa")
@RequiredArgsConstructor
public class ControleMigracaoPessoaController {private final ControleMigracaoPessoaService service;

    /*
     * Registra a decisão do usuário de não unificar
     * uma pessoa individual.
     *
     * Exemplos de banco:
     *
     * PESSOAS
     * RH
     * SANE
     */
    @PostMapping("/nao-migrar")
    public ResponseEntity<String> registrarNaoMigrar(
            @RequestParam String banco,
            @RequestParam Long cdOrigem) {

        service.registrarNaoMigrar(
                banco,
                cdOrigem
        );

        return ResponseEntity.ok(
                "Pessoa marcada para não migração."
        );
    }
}
