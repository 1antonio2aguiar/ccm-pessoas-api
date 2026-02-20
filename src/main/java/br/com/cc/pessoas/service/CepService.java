package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.cep.*;
import br.com.cc.pessoas.filter.CepFilter;
import br.com.cc.pessoas.repository.*;
import br.com.cc.pessoas.entity.Cep;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CepService {

    @Autowired
    private CepRepository cepRepository;

    @Autowired
    private LogradouroRepository logradouroRepository;

    @Autowired
    private BairroRepository bairroRepository;

    public List<CepDTO> listar(CepFilter filter) {
        return cepRepository.filtrar(filter)
                .stream()
                .map(CepDTO::fromCep)
                .toList();
    }

    public Page<CepDTO> pesquisar(CepFilter filter, Pageable pageable) {
        return cepRepository.filtrar(filter, pageable)
                .map(CepDTO::fromCep);
    }

    @Transactional(readOnly = true)
    public CepDTO findDtoById(Long id) {
        Cep cep = cepRepository.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException("CEP não encontrado. Id: " + id));
        return CepDTO.fromCep(cep);
    }

    public Cep insert(CepCreateDTO dto) {
        Cep cep = new Cep();
        BeanUtils.copyProperties(dto, cep);

        cep.setLogradouro(
                logradouroRepository.findById(dto.logradouroId()).get()
        );

        cep.setBairro(
                bairroRepository.findById(dto.bairroId()).get()
        );

        return cepRepository.save(cep);
    }

    public Cep update(Long id, CepUpdateDTO dto) {
        Cep cep = cepRepository.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException("CEP não encontrado. Id: " + id));

        BeanUtils.copyProperties(dto, cep, "id");
        return cepRepository.save(cep);
    }

    @Transactional
    public void delete(Long id) {
        Cep cep = cepRepository.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException("CEP não encontrado. Id: " + id));
        cepRepository.delete(cep);
    }
}