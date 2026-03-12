package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.endereco.EnderecoCreateDTO;
import br.com.cc.pessoas.dto.endereco.EnderecoDTO;
import br.com.cc.pessoas.dto.endereco.EnderecoUpdateDTO;
import br.com.cc.pessoas.dto.logradouro.LogradouroDTO;
import br.com.cc.pessoas.entity.*;
import br.com.cc.pessoas.entity.enuns.TipoEndereco;
import br.com.cc.pessoas.filter.LogradouroFilter;
import com.pesoas.api.filter.enderecos.EnderecoFilter;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EnderecoService {
    @Autowired
    private EnderecoRepository enderecoRepository;
    @Autowired private CepRepository cepRepository;
    @Autowired private PessoaRepository pessoaRepository;
    @Autowired private LogradouroRepository logradouroRepository;
    @Autowired private BairroRepository bairroRepository;

    public Page<EnderecoDTO> pesquisarComPaginacao(EnderecoFilter filter, Pageable pageable) {
        Page<Endereco> page = enderecoRepository.filtrar(filter, pageable);
        return page.map(EnderecoDTO::fromEndereco);
    }

    public List<EnderecoDTO> pesquisarSemPaginacao(EnderecoFilter filter) {
        return enderecoRepository.filtrar(filter)
                .stream()
                .map(EnderecoDTO::fromEndereco)
                .toList();
    }
    @Transactional(readOnly = true)
    public EnderecoDTO findById(Long id) {
        Endereco endereco = enderecoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Logradouro não encontrado. Id: " + id));
        return EnderecoDTO.fromEndereco(endereco);
    }


    @Transactional(readOnly = true)
    public List<EnderecoDTO> findEnderecosByPessoaId(Long pessoaId) {
        if (!pessoaRepository.existsById(pessoaId)) {
            throw new ObjectNotFoundException("Pessoa com ID " + pessoaId + " não encontrada.");
        }
        // Usa o método corrigido do EnderecoRepository
        List<Endereco> enderecos = enderecoRepository.findByPessoaId(pessoaId);
        return enderecos.stream()
                .map(EnderecoDTO::fromEndereco)
                .collect(Collectors.toList());
    }

    //Insert
    public Endereco insert(EnderecoCreateDTO dados){
        Endereco endereco = new Endereco();
        BeanUtils.copyProperties(dados, endereco, "id");

        //Busco a pessoa
        Pessoa pessoa = pessoaRepository.findById(dados.pessoaId())
                .orElseThrow(() -> new ObjectNotFoundException("Pessoa com ID " + dados.pessoaId() + " não encontrada."));
        endereco.setPessoa(pessoa);

        //Busco cep
        Cep cep = cepRepository.findById(dados.cepId())
                .orElseThrow(() -> new ObjectNotFoundException("CEP com ID " + dados.cepId() + " não encontrado."));
        endereco.setCep(cep);

        //Busco logradouro
        Logradouro logradouro = logradouroRepository.findById(dados.logradouroId())
                .orElseThrow(() -> new ObjectNotFoundException("Logradouro com ID " + dados.logradouroId() + " não encontrado."));
        endereco.setLogradouro(logradouro);

        //Busco bairro
        Bairro bairro = bairroRepository.findById(dados.bairroId())
                .orElseThrow(() -> new ObjectNotFoundException("Bairro com ID " + dados.bairroId() + " não encontrado."));
        endereco.setBairro(bairro);

        // set o enum
        endereco.setTipoEndereco(TipoEndereco.toTipoEnderecoEnum(dados.tipoEndereco()));

        Endereco enderecoInsert = enderecoRepository.save(endereco);
        return enderecoInsert;
    }

    // update
    public Endereco update(Long id, EnderecoUpdateDTO dados){

        Endereco enderecoUpd = enderecoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Endereço não cadastrado. Id: " + id));
        BeanUtils.copyProperties(dados, enderecoUpd, "id");

        //Busco cep
        Cep cep = cepRepository.findById(dados.cepId())
                .orElseThrow(() -> new ObjectNotFoundException("CEP com ID " + dados.cepId() + " não encontrado."));
        enderecoUpd.setCep(cep);

        //Busco logradouro
        Logradouro logradouro = logradouroRepository.findById(dados.logradouroId())
                .orElseThrow(() -> new ObjectNotFoundException("Logradouro com ID " + dados.logradouroId() + " não encontrado."));
        enderecoUpd.setLogradouro(logradouro);

        //Busco bairro
        Bairro bairro = bairroRepository.findById(dados.bairroId())
                .orElseThrow(() -> new ObjectNotFoundException("Bairro com ID " + dados.bairroId() + " não encontrado."));
        enderecoUpd.setBairro(bairro);

        // set o enum
        enderecoUpd.setTipoEndereco(TipoEndereco.toTipoEnderecoEnum(dados.tipoEndereco()));

        return enderecoRepository.save(enderecoUpd);
    }

    // Delete
    public void delete(Long id){
        Endereco enderecoDel = enderecoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Endereço não cadastrado. Id: " + id));
        try {
            enderecoRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ObjectNotFoundException(id);
        } catch (DataIntegrityViolationException e){
            throw new DatabaseException(e.getMessage());
        }
    }

    @Transactional
    public void definirEnderecoComoPrincipal(Long pessoaId, Long enderecoId) {
        // A validação da existência da pessoa já está implícita na busca

        // Esta chamada agora usa a query corrigida que opera em 'pessoa.id'
        enderecoRepository.marcarTodosComoNaoPrincipalParaPessoa(pessoaId);

        Endereco enderecoParaAtualizar = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new ObjectNotFoundException("Endereço com ID " + enderecoId + " não encontrado."));

        // A validação de posse agora é mais simples e robusta
        if (!enderecoParaAtualizar.getPessoa().getId().equals(pessoaId)) {
            throw new IllegalArgumentException("O endereço não pertence à pessoa especificada.");
        }

        enderecoParaAtualizar.setPrincipal("S");
        enderecoRepository.save(enderecoParaAtualizar);
    }
}