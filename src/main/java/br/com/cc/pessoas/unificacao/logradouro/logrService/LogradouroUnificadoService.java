package br.com.cc.pessoas.unificacao.logradouro.logrService;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroCandidatoDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroCepDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroOrigemDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroUnificadoLoteDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.LogradouroUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrDto.OrigemLogradouroDTO;
import br.com.cc.pessoas.unificacao.logradouro.logrEnum.OrigemLogradouro;
import br.com.cc.pessoas.unificacao.logradouro.logrFilter.LogradouroUnificadoFilter;
import br.com.cc.pessoas.unificacao.logradouro.logrRepository.LogradouroUnificadoRepository;
import br.com.cc.pessoas.unificacao.logradouro.logrRepository.LogradouroUnificadoRepository.LogradouroOrigemDados;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Unificação de logradouros por sistema de origem.
 *
 * Substitui SP_LOGRADOUROS_SEPARA_NOME*, SP_LOGRADOUROS_GERA_UNIFICADO,
 * SP_LOGRADOUROS_GERA_UNI_ESCOLA e SP_LOGRADOUROS_UNIFICA com processamento
 * incremental: nada é apagado, códigos já gravados não mudam, cada logradouro
 * é tratado na própria transação e só é comparado com os logradouros do
 * mesmo distrito dos Correios.
 *
 * Logradouro novo só é criado com um CEP que já existe na tabela de CEPs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogradouroUnificadoService {

    private final LogradouroUnificadoRepository repository;

    public List<OrigemLogradouroDTO> listarOrigens() {
        return Arrays.stream(OrigemLogradouro.values())
                .map(OrigemLogradouroDTO::fromEnum)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LogradouroOrigemDTO> filtrarPendentes(
            String origem,
            LogradouroUnificadoFilter filter,
            Pageable pageable) {

        return repository.filtrarPendentes(OrigemLogradouro.from(origem), filter, pageable);
    }

    @Transactional(readOnly = true)
    public LogradouroUnificadoResumoDTO buscarResumo(String origem) {
        return repository.buscarResumo(OrigemLogradouro.from(origem));
    }

    @Transactional(readOnly = true)
    public List<LogradouroCandidatoDTO> buscarCandidatos(
            String origem,
            Long cidade,
            Long distrito,
            Long logradouro,
            String pesquisa,
            boolean todos) {

        OrigemLogradouro origemLogradouro = OrigemLogradouro.from(origem);

        LogradouroOrigemDados dados =
                buscarLogradouroOrigem(origemLogradouro, cidade, distrito, logradouro);

        return repository.buscarCandidatos(origemLogradouro, dados, pesquisa, todos);
    }

    @Transactional(readOnly = true)
    public List<LogradouroCepDTO> buscarCeps(
            String origem,
            Long cidade,
            Long distrito,
            Long logradouro,
            String pesquisa) {

        OrigemLogradouro origemLogradouro = OrigemLogradouro.from(origem);

        LogradouroOrigemDados dados =
                buscarLogradouroOrigem(origemLogradouro, cidade, distrito, logradouro);

        validarDistritoUnificado(dados);

        return repository.buscarCeps(dados, pesquisa);
    }

    /**********************************************************************
     * VINCULAR
     **********************************************************************/

    @Transactional
    public String vincular(
            String origem,
            Long cidade,
            Long distrito,
            Long logradouro,
            Long idUnificado) {

        boolean duplicado =
                vincularInterno(
                        OrigemLogradouro.from(origem),
                        cidade,
                        distrito,
                        logradouro,
                        idUnificado
                );

        String chave = cidade + "/" + distrito + "/" + logradouro;

        if (duplicado) {
            return "Logradouro " + chave
                    + " registrado como código duplicado do logradouro unificado "
                    + idUnificado
                    + ", que já possui outro código desta origem.";
        }

        return "Logradouro " + chave + " vinculado ao logradouro unificado " + idUnificado + ".";
    }

    /*
     * Lote em conjunto: três comandos SQL numa única transação
     * (ver LogradouroUnificadoRepository, "LOTE EM CONJUNTO").
     *
     * Mesmo resultado do processamento um a um: o primeiro pendente de cada
     * unificado é vinculado, os demais viram duplicados (D) e as colunas
     * vazias são completadas. Se algo falhar, nada é gravado.
     */
    @Transactional
    public LogradouroUnificadoLoteDTO vincularUnicos(String origem) {

        OrigemLogradouro origemX = OrigemLogradouro.from(origem);

        long duplicados = repository.registrarDuplicadosDoLote(origemX);
        long vinculados = repository.vincularLote(origemX);
        long completados = repository.complementarVinculados(origemX);

        log.info(
                "Lote de logradouros da origem {}: {} vinculados, {} duplicados, {} completados.",
                origemX,
                vinculados,
                duplicados,
                completados
        );

        return new LogradouroUnificadoLoteDTO(
                vinculados + duplicados,
                vinculados,
                duplicados,
                0L,
                new ArrayList<>()
        );
    }

    /*
     * Retorna true quando o logradouro unificado já tinha outro código
     * desta origem e o vínculo foi registrado como duplicado.
     */
    protected boolean vincularInterno(
            OrigemLogradouro origem,
            Long cidade,
            Long distrito,
            Long logradouro,
            Long idUnificado) {

        if (idUnificado == null) {
            throw new NegocioException("Logradouro unificado não informado.");
        }

        LogradouroOrigemDados dados =
                buscarLogradouroOrigem(origem, cidade, distrito, logradouro);

        validarNaoTratado(origem, cidade, distrito, logradouro);
        validarDistritoUnificado(dados);

        Long cidadeCorreiosUnificado =
                repository.buscarCidadeCorreiosDoUnificado(idUnificado);

        if (cidadeCorreiosUnificado == null || cidadeCorreiosUnificado < 0) {
            throw new NegocioException("Logradouro unificado " + idUnificado + " não encontrado.");
        }

        if (!cidadeCorreiosUnificado.equals(dados.cidadeCorreios())) {
            throw new NegocioException(
                    "O logradouro unificado " + idUnificado
                            + " pertence à cidade dos Correios " + cidadeCorreiosUnificado
                            + ", mas o logradouro da origem está na cidade "
                            + dados.cidadeCorreios() + "."
            );
        }

        int alterados = repository.vincularLogradouro(origem, idUnificado, dados);

        if (alterados > 0) {
            repository.complementarLogradouro(origem, idUnificado, dados);
            return false;
        }

        repository.registrarControle(
                origem,
                cidade,
                distrito,
                logradouro,
                LogradouroUnificadoRepository.STATUS_DUPLICADO,
                idUnificado,
                "Código duplicado na origem.",
                null
        );

        return true;
    }

    /**********************************************************************
     * CRIAR LOGRADOURO NOVO
     **********************************************************************/

    @Transactional
    public String criar(
            String origem,
            Long cidade,
            Long distrito,
            Long logradouro,
            Long cep) {

        OrigemLogradouro origemLogradouro = OrigemLogradouro.from(origem);

        LogradouroOrigemDados dados =
                buscarLogradouroOrigem(origemLogradouro, cidade, distrito, logradouro);

        validarNaoTratado(origemLogradouro, cidade, distrito, logradouro);
        validarDistritoUnificado(dados);

        if (dados.nome() == null || dados.nome().isBlank()) {
            throw new NegocioException("O logradouro da origem está sem nome.");
        }

        if (cep == null || cep <= 0L) {
            throw new NegocioException(
                    "Escolha um CEP da tabela de CEPs dos Correios para o logradouro novo."
            );
        }

        Long bairroCep = repository.buscarBairroDoCepValido(dados.cidadeCorreios(), cep);

        if (bairroCep == null || bairroCep < 0L) {
            throw new NegocioException(
                    "O CEP " + cep + " não existe na tabela de CEPs dos Correios para "
                            + dados.nomeCidade() + "/" + dados.uf()
                            + ". A tela não cria CEP: escolha um CEP da lista."
            );
        }

        long compativeis =
                repository.buscarCandidatos(origemLogradouro, dados, null, false)
                        .stream()
                        .filter(c -> "NOME".equals(c.criterio()))
                        .count();

        if (compativeis > 0) {
            throw new NegocioException(
                    "Já existe(m) " + compativeis
                            + " logradouro(s) com o nome " + dados.nome()
                            + " neste distrito. Utilize a opção de vincular."
            );
        }

        Long idUnificado =
                repository.criarLogradouro(
                        origemLogradouro,
                        dados,
                        cep,
                        bairroCep == 0L ? null : bairroCep
                );

        repository.complementarLogradouro(origemLogradouro, idUnificado, dados);

        return "Logradouro " + nvl(dados.tipo()) + " " + dados.nome()
                + " criado em " + dados.nomeCidade() + "/" + dados.uf()
                + " com o CEP " + cep
                + " (logradouro unificado " + idUnificado + ").";
    }

    /**********************************************************************
     * NÃO VINCULAR
     **********************************************************************/

    @Transactional
    public String naoVincular(
            String origem,
            Long cidade,
            Long distrito,
            Long logradouro,
            String motivo) {

        OrigemLogradouro origemLogradouro = OrigemLogradouro.from(origem);

        buscarLogradouroOrigem(origemLogradouro, cidade, distrito, logradouro);
        validarNaoTratado(origemLogradouro, cidade, distrito, logradouro);

        repository.registrarControle(
                origemLogradouro,
                cidade,
                distrito,
                logradouro,
                LogradouroUnificadoRepository.STATUS_NAO_VINCULAR,
                null,
                motivo == null || motivo.isBlank() ? null : motivo.trim(),
                null
        );

        return "Logradouro " + cidade + "/" + distrito + "/" + logradouro
                + " retirado da lista de unificação.";
    }

    /**********************************************************************
     * VALIDAÇÕES
     **********************************************************************/

    private LogradouroOrigemDados buscarLogradouroOrigem(
            OrigemLogradouro origem,
            Long cidade,
            Long distrito,
            Long logradouro) {

        if (cidade == null || distrito == null || logradouro == null) {
            throw new NegocioException("Cidade, distrito e logradouro devem ser informados.");
        }

        LogradouroOrigemDados dados =
                repository.buscarLogradouroOrigem(origem, cidade, distrito, logradouro);

        if (dados == null) {
            throw new NegocioException(
                    "Logradouro " + cidade + "/" + distrito + "/" + logradouro
                            + " não encontrado na origem " + origem.getDescricao() + "."
            );
        }

        return dados;
    }

    private void validarNaoTratado(
            OrigemLogradouro origem,
            Long cidade,
            Long distrito,
            Long logradouro) {

        if (repository.origemJaTratada(origem, cidade, distrito, logradouro)) {
            throw new NegocioException(
                    "O logradouro " + cidade + "/" + distrito + "/" + logradouro
                            + " da origem " + origem.getDescricao()
                            + " já foi unificado ou possui decisão registrada."
            );
        }
    }

    private void validarDistritoUnificado(LogradouroOrigemDados dados) {

        if (dados.cidadeCorreios() != null && dados.distritoCorreios() != null) {
            return;
        }

        if (dados.qtdDistritosCorreios() != null && dados.qtdDistritosCorreios() > 1) {
            throw new NegocioException(
                    "O distrito " + dados.cidade() + "/" + dados.distrito()
                            + " está unificado com mais de um distrito dos Correios."
                            + " Corrija na tela de distritos."
            );
        }

        throw new NegocioException(
                "O distrito " + dados.cidade() + "/" + dados.distrito()
                        + " ainda não foi unificado. Unifique o distrito primeiro."
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

    private static String nvl(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
