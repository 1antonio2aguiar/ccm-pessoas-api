package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.contato.ContatoCreateDTO;
import br.com.cc.pessoas.dto.contato.ContatoDTO;
import br.com.cc.pessoas.dto.contato.ContatoUpdateDTO;
import br.com.cc.pessoas.entity.*;
import br.com.cc.pessoas.entity.enuns.TipoContato;
import br.com.cc.pessoas.filter.ContatoFilter;
import br.com.cc.pessoas.repository.*;
import br.com.cc.pessoas.service.exceptions.DatabaseException;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContatoService {
    @Autowired
    private ContatoRepository contatoRepository;
    @Autowired private PessoaRepository pessoaRepository;

    public Page<ContatoDTO> pesquisarComPaginacao(ContatoFilter filter, Pageable pageable) {
        Page<Contato> page = contatoRepository.filtrar(filter, pageable);
        return page.map(ContatoDTO::fromContato);
    }

    public List<ContatoDTO> pesquisarSemPaginacao(ContatoFilter filter) {
        return contatoRepository.filtrar(filter)
                .stream()
                .map(ContatoDTO::fromContato)
                .toList();
    }
    @Transactional(readOnly = true)
    public ContatoDTO findById(Long id) {
        Contato contato = contatoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Contato não encontrado. Id: " + id));
        return ContatoDTO.fromContato(contato);
    }


    @Transactional(readOnly = true)
    public List<ContatoDTO> findContatoByPessoaId(Long pessoaId) {
        if (!pessoaRepository.existsById(pessoaId)) {
            throw new ObjectNotFoundException("Pessoa com ID " + pessoaId + " não encontrada.");
        }
        // Usa o método corrigido do contatoRepository
        List<Contato> contatos = contatoRepository.findByPessoaId(pessoaId);
        return contatos.stream()
                .map(ContatoDTO::fromContato)
                .collect(Collectors.toList());
    }

    //Insert
    public Contato insert(ContatoCreateDTO dados){
        Contato contato = new Contato();
        BeanUtils.copyProperties(dados, contato, "id");

        //Busco a pessoa
        Pessoa pessoa = pessoaRepository.findById(dados.pessoaId())
                .orElseThrow(() -> new ObjectNotFoundException("Pessoa com ID " + dados.pessoaId() + " não encontrada."));
        contato.setPessoa(pessoa);

        // set o enum
        contato.setTipoContato(TipoContato.toTipoContatoEnum(dados.tipoContato()));

        Contato contatoInsert = contatoRepository.save(contato);
        return contatoInsert;
    }

    // update
    public Contato update(Long id, ContatoUpdateDTO dados){

        Contato contatoUpd = contatoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Contato não cadastrado. Id: " + id));
        BeanUtils.copyProperties(dados, contatoUpd, "id");

        return contatoRepository.save(contatoUpd);
    }

    // Delete
    public void delete(Long id){
        Contato contatoDel = contatoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Contato não cadastrado. Id: " + id));
        try {
            contatoRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ObjectNotFoundException(id);
        } catch (DataIntegrityViolationException e){
            throw new DatabaseException(e.getMessage());
        }
    }

    @Transactional
    public void definirContatoComoPrincipal(Long pessoaId, Long contatoId) {
        // A validação da existência da pessoa já está implícita na busca

        // Esta chamada agora usa a query corrigida que opera em 'pessoa.id'
        contatoRepository.marcarTodosComoNaoPrincipalParaPessoa(pessoaId);

        Contato contatoParaAtualizar = contatoRepository.findById(contatoId)
                .orElseThrow(() -> new ObjectNotFoundException("Contato com ID " + contatoId + " não encontrado."));

        // A validação de posse agora é mais simples e robusta
        if (!contatoParaAtualizar.getPessoa().getId().equals(pessoaId)) {
            throw new IllegalArgumentException("O contato não pertence à pessoa especificada.");
        }

        contatoParaAtualizar.setPrincipal("S");
        contatoRepository.save(contatoParaAtualizar);
    }
}