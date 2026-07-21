package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.contato.ContatoCreateDTO;
import br.com.cc.pessoas.dto.contato.ContatoDTO;
import br.com.cc.pessoas.dto.contato.ContatoUpdateDTO;
import br.com.cc.pessoas.entity.Contato;
import br.com.cc.pessoas.entity.DadosPessoaJuridica;
import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.entity.enuns.TipoContato;
import br.com.cc.pessoas.filter.ContatoFilter;
import br.com.cc.pessoas.repository.ContatoRepository;
import br.com.cc.pessoas.repository.PessoaRepository;
import br.com.cc.pessoas.service.exceptions.DatabaseException;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
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
public class ContatoService {

    @Autowired
    private ContatoRepository contatoRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @PersistenceContext
    private EntityManager manager;

    // =========================================================
    // PESQUISAS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<ContatoDTO> pesquisarComPaginacao(
            ContatoFilter filter,
            Pageable pageable) {

        return contatoRepository
                .filtrar(filter, pageable)
                .map(ContatoDTO::fromContato);
    }

    @Transactional(readOnly = true)
    public List<ContatoDTO> pesquisarSemPaginacao(
            ContatoFilter filter) {

        return contatoRepository.filtrar(filter)
                .stream()
                .map(ContatoDTO::fromContato)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContatoDTO findById(Long id) {

        Contato contato = buscarContato(id);

        return ContatoDTO.fromContato(contato);
    }

    @Transactional(readOnly = true)
    public List<ContatoDTO> findContatoByPessoaId(
            Long pessoaId) {

        validarExistenciaPessoa(pessoaId);

        return contatoRepository.findByPessoaId(pessoaId)
                .stream()
                .map(ContatoDTO::fromContato)
                .toList();
    }

    // =========================================================
    // INSERT
    // =========================================================

    @Transactional
    public Contato insert(ContatoCreateDTO dados) {

        Pessoa pessoa = buscarPessoa(dados.pessoaId());

        Contato contato = new Contato();

        contato.setPessoa(pessoa);

        contato.setDadosPessoaJuridica(
                buscarEstabelecimentoDaPessoa(
                        dados.dadosPessoaJuridicaId(),
                        pessoa
                )
        );

        preencherDadosContato(
                contato,
                dados.contato(),
                dados.tipoContato(),
                dados.principal(),
                dados.complemento()
        );

        if ("S".equalsIgnoreCase(contato.getPrincipal())) {
            desmarcarPrincipalDoMesmoTitular(contato);
        }

        return contatoRepository.save(contato);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Transactional
    public Contato update(
            Long id,
            ContatoUpdateDTO dados) {

        Contato contato = buscarContato(id);

        Pessoa pessoa = contato.getPessoa();

        contato.setDadosPessoaJuridica(
                buscarEstabelecimentoDaPessoa(
                        dados.dadosPessoaJuridicaId(),
                        pessoa
                )
        );

        System.err.println("Principal >>>>>>>>>>>>>>>>>>>> " + dados.principal());

        preencherDadosContato(
                contato,
                dados.contato(),
                dados.tipoContato(),
                dados.principal(),
                dados.complemento()
        );

        if ("S".equalsIgnoreCase(contato.getPrincipal())) {
            desmarcarPrincipalDoMesmoTitular(contato);
        }

        return contatoRepository.save(contato);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void delete(Long id) {

        buscarContato(id);

        try {
            contatoRepository.deleteById(id);
            contatoRepository.flush();

        } catch (EmptyResultDataAccessException e) {
            throw new ObjectNotFoundException(
                    "Contato não encontrado. Id: " + id
            );

        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    // =========================================================
    // DEFINIR COMO PRINCIPAL
    // =========================================================

    @Transactional
    public void definirContatoComoPrincipal(
            Long pessoaId,
            Long contatoId) {

        Contato contato = buscarContato(contatoId);

        if (contato.getPessoa() == null
                || !contato.getPessoa().getId().equals(pessoaId)) {

            throw new IllegalArgumentException(
                    "O contato não pertence à pessoa informada."
            );
        }

        desmarcarPrincipalDoMesmoTitular(contato);

        contato.setPrincipal("S");

        contatoRepository.save(contato);
    }

    /**
     * Cada titular possui seu contato principal:
     *
     * dadosPessoaJuridica == null:
     * contato principal do CPF.
     *
     * dadosPessoaJuridica != null:
     * contato principal daquele estabelecimento.
     */
    private void desmarcarPrincipalDoMesmoTitular(
            Contato contato) {

        Long pessoaId = contato.getPessoa().getId();
        Long contatoId = contato.getId();

        DadosPessoaJuridica estabelecimento =
                contato.getDadosPessoaJuridica();

        TipoContato tipoContato =
                contato.getTipoContato();

        if (tipoContato == null) {
            throw new IllegalArgumentException(
                    "O tipo do contato é obrigatório para definir o principal."
            );
        }

        if (estabelecimento == null) {

            contatoRepository
                    .marcarTodosComoNaoPrincipalParaPessoaFisica(
                            pessoaId,
                            tipoContato,
                            contatoId
                    );

            return;
        }

        contatoRepository
                .marcarTodosComoNaoPrincipalParaEstabelecimento(
                        pessoaId,
                        estabelecimento.getId(),
                        tipoContato,
                        contatoId
                );
    }

    // =========================================================
    // PREENCHIMENTO
    // =========================================================

    private void preencherDadosContato(
            Contato contato,
            String valorContato,
            Integer tipoContato,
            String principal,
            String complemento) {

        contato.setContato(valorContato);

        contato.setTipoContato(
                TipoContato.toTipoContatoEnum(tipoContato)
        );

        contato.setPrincipal(
                "S".equalsIgnoreCase(principal)
                        ? "S"
                        : "N"
        );

        contato.setComplemento(complemento);
    }

    // =========================================================
    // BUSCAS E VALIDAÇÕES
    // =========================================================

    private Contato buscarContato(Long id) {

        return contatoRepository.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException(
                                "Contato não encontrado. Id: " + id
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
     * Retorna null quando o contato pertence ao CPF.
     *
     * Quando o ID estiver preenchido, valida se o estabelecimento
     * realmente pertence à pessoa.
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
}