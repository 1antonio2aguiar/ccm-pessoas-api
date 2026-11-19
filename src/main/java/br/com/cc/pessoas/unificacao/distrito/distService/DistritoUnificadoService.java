package br.com.cc.pessoas.unificacao.distrito.distService;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoCandidatoDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoOrigemDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoUnificadoLoteDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.DistritoUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.distrito.distDto.OrigemDistritoDTO;
import br.com.cc.pessoas.unificacao.distrito.distEnum.OrigemDistrito;
import br.com.cc.pessoas.unificacao.distrito.distFilter.DistritoUnificadoFilter;
import br.com.cc.pessoas.unificacao.distrito.distRepository.DistritoUnificadoRepository;
import br.com.cc.pessoas.unificacao.distrito.distRepository.DistritoUnificadoRepository.DistritoOrigemDados;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Unificação de distritos por sistema de origem.
 *
 * Substitui a SP_DISTRITOS_GERA_UNIFICADO com processamento incremental:
 * nada é apagado, códigos já gravados não mudam, cada distrito é tratado
 * na própria transação e só é comparado com os distritos da mesma cidade.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistritoUnificadoService {

    private static final int MAX_ERROS_RETORNADOS = 50;

    private final DistritoUnificadoRepository repository;
    private final TransactionTemplate transactionTemplate;

    public List<OrigemDistritoDTO> listarOrigens() {
        return Arrays.stream(OrigemDistrito.values())
                .map(OrigemDistritoDTO::fromEnum)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<DistritoOrigemDTO> filtrarPendentes(
            String origem,
            DistritoUnificadoFilter filter,
            Pageable pageable) {

        return repository.filtrarPendentes(
                OrigemDistrito.from(origem),
                filter,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public DistritoUnificadoResumoDTO buscarResumo(String origem) {
        return repository.buscarResumo(OrigemDistrito.from(origem));
    }

    @Transactional(readOnly = true)
    public List<DistritoCandidatoDTO> buscarCandidatos(
            String origem,
            Long cidade,
            Long distrito,
            String pesquisa,
            boolean todos) {

        OrigemDistrito origemDistrito = OrigemDistrito.from(origem);

        DistritoOrigemDados dados =
                buscarDistritoOrigem(origemDistrito, cidade, distrito);

        return repository.buscarCandidatos(
                origemDistrito,
                dados,
                pesquisa,
                todos
        );
    }

    /**********************************************************************
     * VINCULAR
     **********************************************************************/

    @Transactional
    public String vincular(
            String origem,
            Long cidade,
            Long distrito,
            Long idUnificado) {

        boolean duplicado =
                vincularInterno(
                        OrigemDistrito.from(origem),
                        cidade,
                        distrito,
                        idUnificado
                );

        String chave = cidade + "/" + distrito;

        if (duplicado) {
            return "Distrito " + chave
                    + " registrado como código duplicado do distrito unificado "
                    + idUnificado
                    + ", que já possui outro código desta origem.";
        }

        return "Distrito " + chave
                + " vinculado ao distrito unificado "
                + idUnificado + ".";
    }

    /*
     * Lista lida uma única vez; cada distrito em transação própria.
     */
    public DistritoUnificadoLoteDTO vincularUnicos(String origem) {

        OrigemDistrito origemDistrito = OrigemDistrito.from(origem);

        List<Long[]> unicos = repository.listarUnicos(origemDistrito);

        long vinculados = 0;
        long duplicados = 0;
        long totalErros = 0;
        List<String> erros = new ArrayList<>();

        for (Long[] item : unicos) {

            Long cidade = item[0];
            Long distrito = item[1];
            Long idUnificado = item[2];

            try {

                Boolean duplicado =
                        transactionTemplate.execute(status ->
                                vincularInterno(
                                        origemDistrito,
                                        cidade,
                                        distrito,
                                        idUnificado
                                )
                        );

                if (Boolean.TRUE.equals(duplicado)) {
                    duplicados++;
                } else {
                    vinculados++;
                }

            } catch (Exception e) {

                totalErros++;

                log.error(
                        "Erro ao vincular o distrito {}/{} da origem {}.",
                        cidade,
                        distrito,
                        origemDistrito,
                        e
                );

                if (erros.size() < MAX_ERROS_RETORNADOS) {
                    erros.add(
                            "Distrito " + cidade + "/" + distrito + ": "
                                    + obterMensagemErro(e)
                    );
                }
            }
        }

        return new DistritoUnificadoLoteDTO(
                (long) unicos.size(),
                vinculados,
                duplicados,
                totalErros,
                erros
        );
    }

    /*
     * Retorna true quando o distrito unificado já tinha outro código
     * desta origem e o vínculo foi registrado como duplicado.
     */
    protected boolean vincularInterno(
            OrigemDistrito origem,
            Long cidade,
            Long distrito,
            Long idUnificado) {

        if (idUnificado == null) {
            throw new NegocioException("Distrito unificado não informado.");
        }

        DistritoOrigemDados dados =
                buscarDistritoOrigem(origem, cidade, distrito);

        validarNaoTratado(origem, cidade, distrito);
        validarCidadeUnificada(dados);

        Long cidadeCorreiosUnificado =
                repository.buscarCidadeCorreiosDoUnificado(idUnificado);

        if (cidadeCorreiosUnificado == null || cidadeCorreiosUnificado < 0) {
            throw new NegocioException(
                    "Distrito unificado " + idUnificado + " não encontrado."
            );
        }

        if (!cidadeCorreiosUnificado.equals(dados.cidadeCorreios())) {
            throw new NegocioException(
                    "O distrito unificado " + idUnificado
                            + " pertence à cidade dos Correios " + cidadeCorreiosUnificado
                            + ", mas a cidade do distrito na origem está unificada com a cidade "
                            + dados.cidadeCorreios() + "."
            );
        }

        int alterados = repository.vincularDistrito(origem, idUnificado, dados);

        if (alterados > 0) {
            return false;
        }

        repository.registrarControle(
                origem,
                cidade,
                distrito,
                DistritoUnificadoRepository.STATUS_DUPLICADO,
                idUnificado,
                "Código duplicado na origem.",
                null
        );

        return true;
    }

    /**********************************************************************
     * CRIAR DISTRITO NOVO
     **********************************************************************/

    @Transactional
    public String criar(
            String origem,
            Long cidade,
            Long distrito) {

        OrigemDistrito origemDistrito = OrigemDistrito.from(origem);

        DistritoOrigemDados dados =
                buscarDistritoOrigem(origemDistrito, cidade, distrito);

        validarNaoTratado(origemDistrito, cidade, distrito);
        validarCidadeUnificada(dados);

        List<DistritoCandidatoDTO> candidatos =
                repository.buscarCandidatos(origemDistrito, dados, null, false);

        if (!candidatos.isEmpty()) {
            throw new NegocioException(
                    "Já existe(m) " + candidatos.size()
                            + " distrito(s) com o nome " + dados.nome()
                            + " nesta cidade. Utilize a opção de vincular."
            );
        }

        Long idUnificado = repository.criarDistrito(origemDistrito, dados);

        return "Distrito " + dados.nome()
                + " criado na cidade " + dados.nomeCidade() + "/" + dados.uf()
                + " (distrito unificado " + idUnificado + ").";
    }

    /**********************************************************************
     * NÃO VINCULAR
     **********************************************************************/

    @Transactional
    public String naoVincular(
            String origem,
            Long cidade,
            Long distrito,
            String motivo) {

        OrigemDistrito origemDistrito = OrigemDistrito.from(origem);

        buscarDistritoOrigem(origemDistrito, cidade, distrito);
        validarNaoTratado(origemDistrito, cidade, distrito);

        repository.registrarControle(
                origemDistrito,
                cidade,
                distrito,
                DistritoUnificadoRepository.STATUS_NAO_VINCULAR,
                null,
                motivo == null || motivo.isBlank() ? null : motivo.trim(),
                null
        );

        return "Distrito " + cidade + "/" + distrito
                + " retirado da lista de unificação.";
    }

    /**********************************************************************
     * VALIDAÇÕES
     **********************************************************************/

    private DistritoOrigemDados buscarDistritoOrigem(
            OrigemDistrito origem,
            Long cidade,
            Long distrito) {

        if (cidade == null || distrito == null) {
            throw new NegocioException("Cidade e distrito devem ser informados.");
        }

        DistritoOrigemDados dados =
                repository.buscarDistritoOrigem(origem, cidade, distrito);

        if (dados == null) {
            throw new NegocioException(
                    "Distrito " + cidade + "/" + distrito
                            + " não encontrado na origem " + origem.getDescricao() + "."
            );
        }

        return dados;
    }

    private void validarNaoTratado(
            OrigemDistrito origem,
            Long cidade,
            Long distrito) {

        if (repository.origemJaTratada(origem, cidade, distrito)) {
            throw new NegocioException(
                    "O distrito " + cidade + "/" + distrito
                            + " da origem " + origem.getDescricao()
                            + " já foi unificado ou possui decisão registrada."
            );
        }
    }

    private void validarCidadeUnificada(DistritoOrigemDados dados) {

        if (dados.cidadeCorreios() != null) {
            return;
        }

        if (dados.qtdCidadesCorreios() != null && dados.qtdCidadesCorreios() > 1) {
            throw new NegocioException(
                    "A cidade " + dados.cidade()
                            + " está unificada com mais de uma cidade dos Correios."
                            + " Corrija na tela de cidades."
            );
        }

        throw new NegocioException(
                "A cidade " + dados.cidade()
                        + (dados.nomeCidade() == null ? "" : " (" + dados.nomeCidade() + ")")
                        + " ainda não foi unificada. Unifique a cidade primeiro."
        );
    }

    private String obterMensagemErro(Throwable erro) {

        if (erro == null) {
            return "Erro não identificado.";
        }

        Throwable atual = erro;

        while (atual.getCause() != null) {
            atual = atual.getCause();
        }

        if (atual.getMessage() != null && !atual.getMessage().isBlank()) {
            return atual.getMessage().trim();
        }

        return atual.getClass().getSimpleName();
    }
}
