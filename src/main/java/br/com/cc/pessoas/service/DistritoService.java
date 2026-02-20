package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.distrito.DistritoCreateDTO;
import br.com.cc.pessoas.dto.distrito.DistritoDTO;
import br.com.cc.pessoas.dto.distrito.DistritoUpdateDTO;
import br.com.cc.pessoas.entity.Cidade;
import br.com.cc.pessoas.entity.Distrito;
import br.com.cc.pessoas.filter.DistritoFilter;
import br.com.cc.pessoas.repository.CidadeRepository;
import br.com.cc.pessoas.repository.DistritoRepository;

import br.com.cc.pessoas.service.exceptions.DatabaseException;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DistritoService {

    @Autowired
    private DistritoRepository distritoRepository;
    @Autowired
    private CidadeRepository cidadeRepository;

    public DistritoService(DistritoRepository distritoRepository,
                           CidadeRepository cidadeRepository) {
        this.distritoRepository = distritoRepository;
        this.cidadeRepository = cidadeRepository;
    }

    public List<DistritoDTO> listar(DistritoFilter filter) {
        return distritoRepository.filtrar(filter)
                .stream()
                .map(DistritoDTO::fromDistrito)
                .toList();
    }

    public Page<DistritoDTO> pesquisar(DistritoFilter filter, Pageable pageable) {
        Page<Distrito> page = distritoRepository.filtrar(filter, pageable);
        return page.map(DistritoDTO::fromDistrito);
    }

    @Transactional(readOnly = true)
    public DistritoDTO findDtoById(Long id) {
        Distrito distrito = distritoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Distrito não encontrado. Id: " + id
                ));
        return DistritoDTO.fromDistrito(distrito);
    }

    public Distrito insert(DistritoCreateDTO dto) {

        Distrito distrito = new Distrito();
        distrito.setNome(dto.getNome());
        distrito.setCodigoInep(dto.codigoInep());

        Cidade cidade = cidadeRepository.findById(dto.cidadeId())
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Cidade não encontrada. Id: " + dto.cidadeId()
                ));

        distrito.setCidade(cidade);
        return distritoRepository.save(distrito);
    }

    public Distrito update(Long id, DistritoUpdateDTO dto) {

        Distrito distrito = distritoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Distrito não encontrado. Id: " + id
                ));

        distrito.setNome(dto.getNome());
        distrito.setCodigoInep(dto.codigoInep());

        Cidade cidade = cidadeRepository.findById(dto.cidadeId())
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Cidade não encontrada. Id: " + dto.cidadeId()
                ));

        distrito.setCidade(cidade);
        return distritoRepository.save(distrito);
    }

    // Delete
    public void delete(Long id){
        Distrito distritoDel = distritoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Distrito não cadastrada. Id: " + id));
        try {
            distritoRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ObjectNotFoundException(id);
        } catch (DataIntegrityViolationException e){
            throw new DatabaseException(e.getMessage());
        }
    }
}
