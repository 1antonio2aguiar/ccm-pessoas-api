package br.com.cc.pessoas.unificacao.pessoa.pesController;

import br.com.cc.pessoas.unificacao.pessoa.pesDto.PesPessoaDTO;
import br.com.cc.pessoas.unificacao.pessoa.pesFilter.PesPessoaFilter;
import br.com.cc.pessoas.unificacao.pessoa.pesRepository.PesPessoaRepository;
import br.com.cc.pessoas.unificacao.pessoa.pesService.PesPessoaService;
import br.com.cc.pessoas.unificacao.pessoa.pesService.PesPessoaCpfDuplicadoService;
import br.com.cc.pessoas.unificacao.pessoa.pesService.PesPessoaJaExisteCadUnicoService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@RestController
@RequestMapping("/pessoa/pes-pessoas")
@RequiredArgsConstructor
public class PesPessoaController {

    private final PesPessoaService service;
    private final PesPessoaRepository pesPessoaRepository;
    private final PesPessoaCpfDuplicadoService cpfDuplicadoService;
    private final PesPessoaJaExisteCadUnicoService pessoaJaExisteCadUnicoService;

    @GetMapping("/cpf-unico-nao-migradas")
    public Page<PesPessoaDTO> filtrarCpfUnicoNaoMigradas(
            @ModelAttribute PesPessoaFilter filter,
            Pageable pageable
    ) {
        return pesPessoaRepository
                .filtrarCpfUnicoNaoMigradas(
                        filter,
                        pageable
                );
    }

    @PostMapping("/cpf-unico/processar/{pessoaId}")
    public ResponseEntity<String> processarPessoaCpfUnico(
            @PathVariable Long pessoaId) {

        try {

            boolean processado =
                    service.processarCpfUnicoPorId(
                            pessoaId
                    );

            if (!processado) {
                return ResponseEntity.ok(
                        "Pessoa não processada porque o CPF já existe no Cadastro Único."
                );
            }

            return ResponseEntity.ok(
                    "Pessoa e empresas vinculadas processadas com sucesso."
            );

        } catch (Exception e) {

            log.error(
                    "Erro ao processar a pessoa {}.",
                    pessoaId,
                    e
            );

            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(
                            "Não foi possível processar a pessoa. "
                                    + obterMensagemErro(e)
                                    + " Nenhuma alteração foi realizada."
                    );
        }
    }
    @PostMapping(
            "/cpf-unico/processar-ja-existe/{pessoaId}"
    )
    public ResponseEntity<String>
    processarPessoaCpfUnicoJaExistente(
            @PathVariable Long pessoaId) {

        try {

            pessoaJaExisteCadUnicoService
                    .processarPessoaUnica(
                            pessoaId
                    );

            return ResponseEntity.ok(
                    "Pessoa vinculada ao Cadastro Único com sucesso."
            );

        } catch (Exception e) {

            log.error(
                    "Erro ao vincular a pessoa {} ao Cadastro Único existente.",
                    pessoaId,
                    e
            );

            return ResponseEntity
                    .status(
                            HttpStatus.UNPROCESSABLE_ENTITY
                    )
                    .body(
                            "Não foi possível vincular a pessoa "
                                    + "ao Cadastro Único existente. "
                                    + obterMensagemErro(e)
                                    + " Nenhuma alteração foi realizada."
                    );
        }
    }
    @GetMapping("/cpf-duplicado-nao-migradas")
    public Page<PesPessoaDTO> filtrarCpfDuplicadoNaoMigradas(
            @ModelAttribute PesPessoaFilter filter,
            Pageable pageable
    ) {
        return pesPessoaRepository
                .filtrarCpfDuplicadoNaoMigradas(
                        filter,
                        pageable
                );
    }
    @PostMapping("/cpf-duplicado/processar/{pessoaId}")
    public ResponseEntity<String> processarPessoaCpfDuplicado(
            @PathVariable Long pessoaId) {

        try {

            boolean processado =
                    cpfDuplicadoService
                            .processarCpfDuplicadoPorId(
                                    pessoaId
                            );

            if (!processado) {
                return ResponseEntity.ok(
                        "Grupo não processado porque o CPF já existe no Cadastro Único."
                );
            }

            return ResponseEntity.ok(
                    "Grupo de CPF duplicado processado com sucesso."
            );

        } catch (Exception e) {

            log.error(
                    "Erro ao processar o grupo de CPF duplicado da pessoa {}.",
                    pessoaId,
                    e
            );

            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(
                            "Não foi possível processar o grupo de CPF duplicado. "
                                    + obterMensagemErro(e)
                                    + " Nenhuma alteração foi realizada."
                    );
        }
    }
    @PostMapping(
            "/cpf-duplicado/processar-ja-existe/{pessoaId}"
    )
    public ResponseEntity<String>
    processarPessoaCpfDuplicadoJaExistente(
            @PathVariable Long pessoaId) {

        try {

            cpfDuplicadoService
                    .processarCpfDuplicadoJaExistePorId(
                            pessoaId
                    );

            return ResponseEntity.ok(
                    "Grupo de CPF duplicado vinculado "
                            + "ao Cadastro Único com sucesso."
            );

        } catch (Exception e) {

            log.error(
                    "Erro ao vincular o grupo de CPF duplicado "
                            + "da pessoa {} ao Cadastro Único existente.",
                    pessoaId,
                    e
            );

            return ResponseEntity
                    .status(
                            HttpStatus.UNPROCESSABLE_ENTITY
                    )
                    .body(
                            "Não foi possível vincular o grupo "
                                    + "ao Cadastro Único existente. "
                                    + obterMensagemErro(e)
                                    + " Nenhuma alteração foi realizada."
                    );
        }
    }

    @PostMapping(
            "/cpf-duplicado/nao-migrar/{pessoaId}"
    )
    public ResponseEntity<String>
    registrarGrupoCpfDuplicadoNaoMigrar(
            @PathVariable Long pessoaId) {

        try {

            cpfDuplicadoService
                    .registrarGrupoNaoMigrar(
                            pessoaId
                    );

            return ResponseEntity.ok(
                    "Grupo de CPF duplicado retirado "
                            + "da lista de migração."
            );

        } catch (Exception e) {

            log.error(
                    "Erro ao registrar a decisão de não migrar "
                            + "o grupo de CPF duplicado da pessoa {}.",
                    pessoaId,
                    e
            );

            return ResponseEntity
                    .status(
                            HttpStatus.UNPROCESSABLE_ENTITY
                    )
                    .body(
                            "Não foi possível retirar o grupo "
                                    + "da lista de migração. "
                                    + obterMensagemErro(e)
                    );
        }
    }
    @GetMapping("/cnpj-unico-nao-migradas")
    public Page<PesPessoaDTO> filtrarCnpjUnicoNaoMigradas(
            @ModelAttribute PesPessoaFilter filter,
            Pageable pageable
    ) {
        return pesPessoaRepository
                .filtrarCnpjUnicoNaoMigradas(
                        filter,
                        pageable
                );
    }

    @GetMapping("/cnpj-duplicado-nao-migradas")
    public Page<PesPessoaDTO> filtrarCnpjDuplicadoNaoMigradas(
            @ModelAttribute PesPessoaFilter filter,
            Pageable pageable
    ) {
        return pesPessoaRepository
                .filtrarCnpjDuplicadoNaoMigradas(
                        filter,
                        pageable
                );
    }
    private String obterMensagemErro(
            Throwable erro) {

        if (erro == null) {
            return "Erro não identificado.";
        }

        Throwable causa = erro;

        while (causa.getCause() != null) {
            causa = causa.getCause();
        }

        if (causa.getMessage() == null
                || causa.getMessage().isBlank()) {

            return "Erro não identificado.";
        }

        return causa.getMessage().trim();
    }
}