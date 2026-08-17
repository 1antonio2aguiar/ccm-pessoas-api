package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.endereco.EnderecoCreateDTO;
import br.com.cc.pessoas.dto.endereco.EnderecoDTO;
import br.com.cc.pessoas.dto.endereco.EnderecoUpdateDTO;
import br.com.cc.pessoas.entity.Bairro;
import br.com.cc.pessoas.entity.Cep;
import br.com.cc.pessoas.entity.DadosPessoaJuridica;
import br.com.cc.pessoas.entity.Endereco;
import br.com.cc.pessoas.entity.Logradouro;
import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.entity.enuns.TipoEndereco;
import br.com.cc.pessoas.repository.BairroRepository;
import br.com.cc.pessoas.repository.CepRepository;
import br.com.cc.pessoas.repository.EnderecoRepository;
import br.com.cc.pessoas.repository.LogradouroRepository;
import br.com.cc.pessoas.repository.PessoaRepository;
import br.com.cc.pessoas.service.exceptions.DatabaseException;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import com.pesoas.api.filter.enderecos.EnderecoFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnderecoService {

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private CepRepository cepRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private LogradouroRepository logradouroRepository;

    @Autowired
    private BairroRepository bairroRepository;

    @PersistenceContext
    private EntityManager manager;

    // =========================================================
    // PESQUISA
    // =========================================================

    @Transactional(readOnly = true)
    public Page<EnderecoDTO> pesquisarComPaginacao(
            EnderecoFilter filter,
            Pageable pageable) {

        return enderecoRepository
                .filtrar(filter, pageable)
                .map(EnderecoDTO::fromEndereco);
    }

    @Transactional(readOnly = true)
    public List<EnderecoDTO> pesquisarSemPaginacao(
            EnderecoFilter filter) {

        return enderecoRepository.filtrar(filter)
                .stream()
                .map(EnderecoDTO::fromEndereco)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnderecoDTO findById(Long id) {

        Endereco endereco = buscarEndereco(id);

        return EnderecoDTO.fromEndereco(endereco);
    }

    @Transactional(readOnly = true)
    public List<EnderecoDTO> findEnderecosByPessoaId(
            Long pessoaId) {

        validarExistenciaPessoa(pessoaId);

        return enderecoRepository.findByPessoaId(pessoaId)
                .stream()
                .map(EnderecoDTO::fromEndereco)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<EnderecoDTO> findEnderecosByEstabelecimentoId(
            Long dadosPessoaJuridicaId) {

        DadosPessoaJuridica estabelecimento =
                manager.find(
                        DadosPessoaJuridica.class,
                        dadosPessoaJuridicaId
                );

        if (estabelecimento == null) {
            throw new ObjectNotFoundException(
                    "Estabelecimento com ID "
                            + dadosPessoaJuridicaId
                            + " não encontrado."
            );
        }

        return enderecoRepository
                .findByDadosPessoaJuridicaId(
                        dadosPessoaJuridicaId
                )
                .stream()
                .map(EnderecoDTO::fromEndereco)
                .toList();
    }

    // =========================================================
    // INSERT
    // =========================================================

    @Transactional
    public EnderecoDTO insert(EnderecoCreateDTO dados) {

        Pessoa pessoa = buscarPessoa(dados.pessoaId());

        Endereco endereco = new Endereco();

        endereco.setPessoa(pessoa);

        endereco.setDadosPessoaJuridica(
                buscarEstabelecimentoDaPessoa(
                        dados.dadosPessoaJuridicaId(),
                        pessoa
                )
        );

        preencherDadosBasicos(
                endereco,
                dados.numero(),
                dados.principal(),
                dados.tipoEndereco(),
                dados.complemento()
        );

        endereco.setCep(buscarCep(dados.cepId()));
        endereco.setLogradouro(buscarLogradouro(dados.logradouroId()));
        endereco.setBairro(buscarBairro(dados.bairroId()));

        if ("S".equalsIgnoreCase(endereco.getPrincipal())) {
            desmarcarPrincipalDoMesmoTitular(endereco);
        }

        Endereco enderecoSalvo =
                enderecoRepository.saveAndFlush(endereco);

        return EnderecoDTO.fromEndereco(enderecoSalvo);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    /*@Transactional
    public EnderecoDTO update(
            Long id,
            EnderecoUpdateDTO dados) {

        Endereco endereco = buscarEndereco(id);

        Pessoa pessoa = endereco.getPessoa();

        endereco.setDadosPessoaJuridica(
                buscarEstabelecimentoDaPessoa(
                        dados.dadosPessoaJuridicaId(),
                        pessoa
                )
        );

        preencherDadosBasicos(
                endereco,
                dados.numero(),
                dados.principal(),
                dados.tipoEndereco(),
                dados.complemento()
        );

        endereco.setCep(buscarCep(dados.cepId()));
        endereco.setLogradouro(buscarLogradouro(dados.logradouroId()));
        endereco.setBairro(buscarBairro(dados.bairroId()));

        if ("S".equalsIgnoreCase(endereco.getPrincipal())) {
            desmarcarPrincipalDoMesmoTitular(endereco);
        }

        Endereco enderecoAtualizado =
                enderecoRepository.saveAndFlush(endereco);

        return EnderecoDTO.fromEndereco(enderecoAtualizado);
    }*/

    @Transactional
    public EnderecoDTO update(
            Long id,
            EnderecoUpdateDTO dados) {

        Endereco endereco = buscarEndereco(id);

        Pessoa pessoa = endereco.getPessoa();

        endereco.setDadosPessoaJuridica(
                buscarEstabelecimentoDaPessoa(
                        dados.dadosPessoaJuridicaId(),
                        pessoa
                )
        );

        /*
         * Deve acontecer antes de preencher o endereço atual.
         * Assim, o UPDATE nativo desmarca os endereços do mesmo
         * titular e, depois, o endereço atual recebe "S".
         */
        if ("S".equalsIgnoreCase(dados.principal())) {
            desmarcarPrincipalDoMesmoTitular(endereco);
        }

        preencherDadosBasicos(
                endereco,
                dados.numero(),
                dados.principal(),
                dados.tipoEndereco(),
                dados.complemento()
        );

        endereco.setCep(
                buscarCep(dados.cepId())
        );

        endereco.setLogradouro(
                buscarLogradouro(dados.logradouroId())
        );

        endereco.setBairro(
                buscarBairro(dados.bairroId())
        );

        Endereco enderecoAtualizado =
                enderecoRepository.saveAndFlush(endereco);

        return EnderecoDTO.fromEndereco(
                enderecoAtualizado
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void delete(Long id) {

        buscarEndereco(id);

        try {
            enderecoRepository.deleteById(id);
            enderecoRepository.flush();

        } catch (EmptyResultDataAccessException e) {
            throw new ObjectNotFoundException(
                    "Endereço não encontrado. Id: " + id
            );

        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    // =========================================================
    // ENDEREÇO PRINCIPAL
    // =========================================================

    @Transactional
    public void definirEnderecoComoPrincipal(
            Long pessoaId,
            Long enderecoId) {

        Endereco endereco = buscarEndereco(enderecoId);

        if (endereco.getPessoa() == null
                || !endereco.getPessoa().getId().equals(pessoaId)) {

            throw new IllegalArgumentException(
                    "O endereço não pertence à pessoa informada."
            );
        }

        desmarcarPrincipalDoMesmoTitular(endereco);

        endereco.setPrincipal("S");

        enderecoRepository.save(endereco);
    }

    /**
     * Cada titular possui seu próprio endereço principal:
     *
     * dadosPessoaJuridica == null:
     * principal do CPF.
     *
     * dadosPessoaJuridica != null:
     * principal daquele CNPJ específico.
     */
    private void desmarcarPrincipalDoMesmoTitular(
            Endereco endereco) {

        Long pessoaId = endereco.getPessoa().getId();

        DadosPessoaJuridica estabelecimento =
                endereco.getDadosPessoaJuridica();

        if (estabelecimento == null) {

            manager.createNativeQuery("""
                    UPDATE DBO_CCM_PESSOAS.ENDERECOS
                       SET PRINCIPAL = 'N'
                     WHERE PESSOA_ID = :pessoaId
                       AND DADOS_PJ_ID IS NULL
                    """)
                    .setParameter("pessoaId", pessoaId)
                    .executeUpdate();

            return;
        }

        manager.createNativeQuery("""
                UPDATE DBO_CCM_PESSOAS.ENDERECOS
                   SET PRINCIPAL = 'N'
                 WHERE PESSOA_ID = :pessoaId
                   AND DADOS_PJ_ID = :dadosPessoaJuridicaId
                """)
                .setParameter("pessoaId", pessoaId)
                .setParameter(
                        "dadosPessoaJuridicaId",
                        estabelecimento.getId()
                )
                .executeUpdate();
    }

    // =========================================================
    // PREENCHIMENTO
    // =========================================================

    private void preencherDadosBasicos(
            Endereco endereco,
            Long numero,
            String principal,
            Integer tipoEndereco,
            String complemento) {

        endereco.setNumero(numero);
        endereco.setComplemento(complemento);
        endereco.setPrincipal(
                "S".equalsIgnoreCase(principal) ? "S" : "N"
        );

        endereco.setTipoEndereco(
                TipoEndereco.toTipoEnderecoEnum(tipoEndereco)
        );
    }

    // =========================================================
    // BUSCAS E VALIDAÇÕES
    // =========================================================

    private Endereco buscarEndereco(Long id) {

        return enderecoRepository.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException(
                                "Endereço não encontrado. Id: " + id
                        )
                );
    }

    private Pessoa buscarPessoa(Long pessoaId) {

        return pessoaRepository.findById(pessoaId)
                .orElseThrow(() ->
                        new ObjectNotFoundException(
                                "Pessoa com ID "
                                        + pessoaId
                                        + " não encontrada."
                        )
                );
    }

    private void validarExistenciaPessoa(Long pessoaId) {

        if (!pessoaRepository.existsById(pessoaId)) {
            throw new ObjectNotFoundException(
                    "Pessoa com ID "
                            + pessoaId
                            + " não encontrada."
            );
        }
    }

    /**
     * Retorna null quando o endereço pertence diretamente ao CPF.
     *
     * Quando o ID estiver preenchido, valida se o estabelecimento
     * realmente pertence à pessoa informada.
     */
    private DadosPessoaJuridica buscarEstabelecimentoDaPessoa(
            Long dadosPessoaJuridicaId,
            Pessoa pessoa) {

        if (dadosPessoaJuridicaId == null) {
            return null;
        }

        DadosPessoaJuridica estabelecimento =
                manager.find(
                        DadosPessoaJuridica.class,
                        dadosPessoaJuridicaId
                );

        if (estabelecimento == null) {
            throw new ObjectNotFoundException(
                    "Estabelecimento com ID "
                            + dadosPessoaJuridicaId
                            + " não encontrado."
            );
        }

        if (estabelecimento.getPessoa() == null
                || !estabelecimento
                .getPessoa()
                .getId()
                .equals(pessoa.getId())) {

            throw new IllegalArgumentException(
                    "O estabelecimento informado não pertence à pessoa."
            );
        }

        return estabelecimento;
    }

    private Cep buscarCep(Long cepId) {

        if (cepId == null) {
            return null;
        }

        return cepRepository.findById(cepId)
                .orElseThrow(() ->
                        new ObjectNotFoundException(
                                "CEP com ID "
                                        + cepId
                                        + " não encontrado."
                        )
                );
    }

    private Logradouro buscarLogradouro(Long logradouroId) {

        if (logradouroId == null) {
            throw new IllegalArgumentException(
                    "O logradouro é obrigatório."
            );
        }

        return logradouroRepository.findById(logradouroId)
                .orElseThrow(() ->
                        new ObjectNotFoundException(
                                "Logradouro com ID "
                                        + logradouroId
                                        + " não encontrado."
                        )
                );
    }

    private Bairro buscarBairro(Long bairroId) {

        if (bairroId == null) {
            return null;
        }

        return bairroRepository.findById(bairroId)
                .orElseThrow(() ->
                        new ObjectNotFoundException(
                                "Bairro com ID "
                                        + bairroId
                                        + " não encontrado."
                        )
                );
    }
}