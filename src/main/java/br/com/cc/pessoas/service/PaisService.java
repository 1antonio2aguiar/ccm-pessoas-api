package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.pais.PaisCreateDTO;
import br.com.cc.pessoas.dto.pais.PaisUpdateDTO;
import br.com.cc.pessoas.service.exceptions.DatabaseException;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import br.com.cc.pessoas.dto.pais.PaisDTO;
import br.com.cc.pessoas.entity.Pais;
import br.com.cc.pessoas.filter.PaisFilter;
import br.com.cc.pessoas.repository.PaisRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import java.util.Optional;

@Service
public class PaisService {

    @Autowired private PaisRepository paisrepository;

    public PaisService(PaisRepository paisRepository) {
        this.paisrepository = paisRepository;
    }

    //Metodo filtrar lista
    public List<PaisDTO> listar(PaisFilter filter) {
        List<Pais> lista = paisrepository.filtrar(filter);

        return lista.stream()
                .map(PaisDTO::fromPais)
                .toList();
    }

    //Metodo filtrar paginacao
    public Page<PaisDTO> pesquisar(PaisFilter filter, Pageable pageable) {
        Page<Pais> paisPage = paisrepository.filtrar(filter, pageable);

        // Mapeia a lista de provas para uma lista de DadosListProvasRcd usando o método de fábrica
        List<PaisDTO> paisDTOList = paisPage.getContent().stream()
            .map(PaisDTO::fromPais)
            .collect(Collectors.toList());

        // Cria um novo Page<DadosListProvasRcd> com os dados mapeados
        return new PageImpl<>(paisDTOList, pageable, paisPage.getTotalElements());
    }

    // paises por id
    public Pais findById(Long id){
        Optional<Pais> obj = paisrepository.findById(id);
        return obj.orElseThrow(() -> new ObjectNotFoundException(
                "Pais não encontrado! Id: " + id + " Pais: " + Pais.class.getName()));
    }

    //Insert
    public Pais insert(@Valid PaisCreateDTO dados){
        Pais pais = new Pais();
        BeanUtils.copyProperties(dados, pais, "id");

        Pais paisInsert = paisrepository.save(pais);
        return paisInsert;
    }

    // update
    public Pais update(Long id, PaisUpdateDTO dados){
        Pais paisUpd = paisrepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Pais não cadastrada. Id: " + id));
        BeanUtils.copyProperties(dados, paisUpd, "id");

        return paisrepository.save(paisUpd);
    }

    // Delete
    public void delete(Long id){
        Pais pontuacaoDel = paisrepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Pais não cadastrado. Id: " + id));
        try {
            paisrepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ObjectNotFoundException(id);
        } catch (DataIntegrityViolationException e){
            throw new DatabaseException(e.getMessage());
        }
    }

}