package br.com.cc.pessoas.unificacao.bairro.bairService;

import br.com.cc.pessoas.service.exceptions.NegocioException;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroCandidatoDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroOrigemDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroUnificadoLoteDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.BairroUnificadoResumoDTO;
import br.com.cc.pessoas.unificacao.bairro.bairDto.OrigemBairroDTO;
import br.com.cc.pessoas.unificacao.bairro.bairEnum.OrigemBairro;
import br.com.cc.pessoas.unificacao.bairro.bairFilter.BairroUnificadoFilter;
import br.com.cc.pessoas.unificacao.bairro.bairRepository.BairroUnificadoRepository;
import br.com.cc.pessoas.unificacao.bairro.bairRepository.BairroUnificadoRepository.BairroOrigemDados;
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
 * Unificação de bairros por sistema de origem.
 *
 * Substitui a SP_BAIRROS_GERA_UNIFICADO com processamento incremental:
 * nada é apagado, códigos já gravados não mudam, cada bairro é tratado na
 * própria transação e só é comparado com os bairros do mesmo distrito.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BairroUnificadoService {

    private final BairroUnificadoRepository repository;

    public List<OrigemBairroDTO> listarOrigens() {
        return Arrays.stream(OrigemBairro.values())
                .map(OrigemBairroDTO::fromEnum)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<BairroOrigemDTO> filtrarPendentes(
            String origem,
            BairroUnificadoFilter filter,
            Pageable pageable) {

        return repository.filtrarPendentes(OrigemBairro.from(origem), filter, pageable);
    }

    @Transactional(readOnly = true)
    public BairroUnificadoResumoDTO buscarResumo(String origem) {
        return repository.buscarResumo(OrigemBairro.from(origem));
    }

    @Transactional(readOnly = true)
    public List<BairroCandidatoDTO> buscarCandidatos(
            String origem,
            Long cidade,
            Long distrito,
            Long bairro,
            String pesquisa,
            boolean todos) {

        OrigemBairro origemBairro = OrigemBairro.from(origem);

        BairroOrigemDados dados =
                buscarBairroOrigem(origemBairro, cidade, distrito, bairro);

        return repository.buscarCandidatos(origemBairro, dados, pesquisa, todos);
    }

    /**********************************************************************
     * VINCULAR
     **********************************************************************/

    @Transactional
    public String vincular(
            String origem,
            Long cidade,
            Long distrito,
            Long bairro,
            Long idUnificado) {

        boolean duplicado =
                vincularInterno(
                        OrigemBairro.from(origem),
                        cidade,
                        distrito,
                        bairro,
                        idUnificado
                );

        String chave = cidade + "/" + distrito + "/" + bairro;

        if (duplicado) {
            return "Bairro " + chave
                    + " registrado como código duplicado do bairro unificado "
                    + idUnificado
                    + ", que já possui outro código desta origem.";
        }

        return "Bairro " + chave + " vinculado ao bairro unificado " + idUnificado + ".";
    }

    /*
     * Lote em conjunto: três comandos SQL numa única transação
     * (ver BairroUnificadoRepository, "LOTE EM CONJUNTO").
     *
     * Mesmo resultado do processamento um a um: o primeiro pendente de cada
     * unificado é vinculado, os demais viram duplicados (D) e as colunas
     * vazias são completadas. Se algo falhar, nada é gravado.
     */
    @Transactional
    public BairroUnificadoLoteDTO vincularUnicos(String origem) {

        OrigemBairro origemX = OrigemBairro.from(origem);

        long duplicados = repository.registrarDuplicadosDoLote(origemX);
        long vinculados = repository.vincularLote(origemX);
        long completados = repository.complementarVinculados(origemX);

        log.info(
                "Lote de bairros da origem {}: {} vinculados, {} duplicados, {} completados.",
                origemX,
                vinculados,
                duplicados,
                completados
        );

        return new BairroUnificadoLoteDTO(
                vinculados + duplicados,
                vinculados,
                duplicados,
                0L,
                new ArrayList<>()
        );
    }

    /*
     * Retorna true quando o bairro unificado já tinha outro código
     * desta origem e o vínculo foi registrado como duplicado.
     */
    protected boolean vincularInterno(
            OrigemBairro origem,
            Long cidade,
            Long distrito,
            Long bairro,
            Long idUnificado) {

        if (idUnificado == null) {
            throw new NegocioException("Bairro unificado não informado.");
        }

        BairroOrigemDados dados =
                buscarBairroOrigem(origem, cidade, distrito, bairro);

        validarNaoTratado(origem, cidade, distrito, bairro);
        validarDistritoUnificado(dados);

        Long cidadeCorreiosUnificado =
                repository.buscarCidadeCorreiosDoUnificado(idUnificado);

        if (cidadeCorreiosUnificado == null || cidadeCorreiosUnificado < 0) {
            throw new NegocioException("Bairro unificado " + idUnificado + " não encontrado.");
        }

        if (!cidadeCorreiosUnificado.equals(dados.cidadeCorreios())) {
            throw new NegocioException(
                    "O bairro unificado " + idUnificado
                            + " pertence à cidade dos Correios " + cidadeCorreiosUnificado
                            + ", mas o bairro da origem está na cidade "
                            + dados.cidadeCorreios() + "."
            );
        }

        int alterados = repository.vincularBairro(origem, idUnificado, dados);

        if (alterados > 0) {
            repository.complementarBairro(origem, idUnificado, dados);
            return false;
        }

        repository.registrarControle(
                origem,
                cidade,
                distrito,
                bairro,
                BairroUnificadoRepository.STATUS_DUPLICADO,
                idUnificado,
                "Código duplicado na origem.",
                null
        );

        return true;
    }

    /**********************************************************************
     * CRIAR BAIRRO NOVO
     **********************************************************************/

    @Transactional
    public String criar(
            String origem,
            Long cidade,
            Long distrito,
            Long bairro) {

        OrigemBairro origemBairro = OrigemBairro.from(origem);

        BairroOrigemDados dados =
                buscarBairroOrigem(origemBairro, cidade, distrito, bairro);

        validarNaoTratado(origemBairro, cidade, distrito, bairro);
        validarDistritoUnificado(dados);

        List<BairroCandidatoDTO> candidatos =
                repository.buscarCandidatos(origemBairro, dados, null, false);

        if (!candidatos.isEmpty()) {
            throw new NegocioException(
                    "Já existe(m) " + candidatos.size()
                            + " bairro(s) com o nome " + dados.nome()
                            + " neste distrito. Utilize a opção de vincular."
            );
        }

        Long idUnificado = repository.criarBairro(origemBairro, dados);

        repository.complementarBairro(origemBairro, idUnificado, dados);

        return "Bairro " + dados.nome()
                + " criado em " + dados.nomeCidade() + "/" + dados.uf()
                + " (bairro unificado " + idUnificado + ").";
    }

    /**********************************************************************
     * NÃO VINCULAR
     **********************************************************************/

    @Transactional
    public String naoVincular(
            String origem,
            Long cidade,
            Long distrito,
            Long bairro,
            String motivo) {

        OrigemBairro origemBairro = OrigemBairro.from(origem);

        buscarBairroOrigem(origemBairro, cidade, distrito, bairro);
        validarNaoTratado(origemBairro, cidade, distrito, bairro);

        repository.registrarControle(
                origemBairro,
                cidade,
                distrito,
                bairro,
                BairroUnificadoRepository.STATUS_NAO_VINCULAR,
                null,
                motivo == null || motivo.isBlank() ? null : motivo.trim(),
                null
        );

        return "Bairro " + cidade + "/" + distrito + "/" + bairro
                + " retirado da lista de unificação.";
    }

    /**********************************************************************
     * VALIDAÇÕES
     **********************************************************************/

    private BairroOrigemDados buscarBairroOrigem(
            OrigemBairro origem,
            Long cidade,
            Long distrito,
            Long bairro) {

        if (cidade == null || distrito == null || bairro == null) {
            throw new NegocioException("Cidade, distrito e bairro devem ser informados.");
        }

        BairroOrigemDados dados =
                repository.buscarBairroOrigem(origem, cidade, distrito, bairro);

        if (dados == null) {
            throw new NegocioException(
                    "Bairro " + cidade + "/" + distrito + "/" + bairro
                            + " não encontrado na origem " + origem.getDescricao() + "."
            );
        }

        return dados;
    }

    private void validarNaoTratado(
            OrigemBairro origem,
            Long cidade,
            Long distrito,
            Long bairro) {

        if (repository.origemJaTratada(origem, cidade, distrito, bairro)) {
            throw new NegocioException(
                    "O bairro " + cidade + "/" + distrito + "/" + bairro
                            + " da origem " + origem.getDescricao()
                            + " já foi unificado ou possui decisão registrada."
            );
        }
    }

    private void validarDistritoUnificado(BairroOrigemDados dados) {

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
}
