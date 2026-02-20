package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.tipoLogradouro.TipoLogradouroCreateDTO;
import br.com.cc.pessoas.dto.tipoLogradouro.TipoLogradouroDTO;
import br.com.cc.pessoas.dto.tipoLogradouro.TipoLogradouroUpdateDTO;
import br.com.cc.pessoas.entity.TipoLogradouro;
import br.com.cc.pessoas.filter.TipoLogradouroFilter;
import br.com.cc.pessoas.repository.TipoLogradouroRepository;
import br.com.cc.pessoas.service.exceptions.DatabaseException;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TipoLogradouroService {

    @Autowired private TipoLogradouroRepository tlRepository;

    public TipoLogradouroService(TipoLogradouroRepository tlRepository) {
        this.tlRepository = tlRepository;
    }

    //Metodo filtrar lista
    public List<TipoLogradouroDTO> listar(TipoLogradouroFilter filter) {
        List<TipoLogradouro> lista = tlRepository.filtrar(filter);

        return lista.stream()
                .map(TipoLogradouroDTO::fromEntity)
                .toList();
    }

    //Metodo filtrar paginacao
    public Page<TipoLogradouroDTO> pesquisar(TipoLogradouroFilter filter, Pageable pageable) {
        Page<TipoLogradouro> tlPage = tlRepository.filtrar(filter, pageable);

        // Mapeia a lista de provas para uma lista de DadosListProvasRcd usando o método de fábrica
        List<TipoLogradouroDTO> tlDTOList = tlPage.getContent().stream()
            .map(TipoLogradouroDTO::fromEntity)
            .collect(Collectors.toList());

        // Cria um novo Page<DadosListProvasRcd> com os dados mapeados
        return new PageImpl<>(tlDTOList, pageable, tlPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TipoLogradouroDTO findDtoById(Long id) {
        TipoLogradouro tl = tlRepository.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Tipo logradouro não encontrado. Id: " + id));
        return TipoLogradouroDTO.fromEntity(tl);
    }

    //Insert
    public TipoLogradouro insert(@Valid TipoLogradouroCreateDTO dados){
        TipoLogradouro tipoLogradouro = new TipoLogradouro();
        BeanUtils.copyProperties(dados, tipoLogradouro, "id");

        TipoLogradouro tlInsert = tlRepository.save(tipoLogradouro);
        return tlInsert;
    }

    // update
    public TipoLogradouro update(Long id, TipoLogradouroUpdateDTO dados){
        TipoLogradouro tlUpd = tlRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Tipo logradouro não cadastrada. Id: " + id));
        BeanUtils.copyProperties(dados, tlUpd, "id");

        return tlRepository.save(tlUpd);
    }

    // Delete
    public void delete(Long id){
        TipoLogradouro tlDel = tlRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Tipo logradouro não cadastrado. Id: " + id));
        try {
            tlRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ObjectNotFoundException(id);
        } catch (DataIntegrityViolationException e){
            throw new DatabaseException(e.getMessage());
        }
    }

}