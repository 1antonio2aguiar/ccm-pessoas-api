package br.com.cc.pessoas.unificacao.pessoa.pesService;

import br.com.cc.pessoas.unificacao.controle.ctrlService.ControleMigracaoPessoaService;
import br.com.cc.pessoas.unificacao.pessoa.pesEntity.PesPessoa;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesPessoaCpfDuplicadoService
        extends PesCargaPessoaCpfDuplicadoBaseService {

    private final ControleMigracaoPessoaService
            controleMigracaoPessoaService;

    public PesPessoaCpfDuplicadoService(
            ControleMigracaoPessoaService
                    controleMigracaoPessoaService) {

        this.controleMigracaoPessoaService =
                controleMigracaoPessoaService;
    }

    @Override
    protected void insertDadosEspecificos(
            Long idPessoa,
            DadosGrupo dadosGrupo) {

        inserirDadosPf(
                idPessoa,
                dadosGrupo,
                dadosGrupo.cpfCnpjPrincipal,
                dadosGrupo.estadoCivilDestino,
                dadosGrupo.cidadeNascimentoCcm
        );
    }

    /*
     * Registra como "não migrar" todos os integrantes
     * do grupo duplicado representado pela pessoa
     * selecionada.
     */
    @Transactional
    public void registrarGrupoNaoMigrar(
            Long pessoaId) {

        if (pessoaId == null
                || pessoaId <= 0L) {

            throw new IllegalArgumentException(
                    "O código da pessoa é inválido."
            );
        }

        List<PesPessoa> grupo =
                buscarGrupoDuplicado(
                        pessoaId
                );

        if (grupo == null
                || grupo.size() < 2) {

            throw new IllegalArgumentException(
                    "Não foi encontrado um grupo de CPF "
                            + "duplicado para a pessoa "
                            + pessoaId + "."
            );
        }

        List<Long> codigosOrigem =
                grupo.stream()
                        .map(
                                PesPessoa::getPessoa
                        )
                        .toList();

        controleMigracaoPessoaService
                .registrarGrupoNaoMigrar(
                        "PESSOAS",
                        codigosOrigem
                );
    }
}