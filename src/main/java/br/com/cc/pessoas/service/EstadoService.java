package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.estado.EstadoCreateDTO;
import br.com.cc.pessoas.dto.estado.EstadoDTO;
import br.com.cc.pessoas.dto.estado.EstadoUpdateDTO;
import br.com.cc.pessoas.entity.Estado;
import br.com.cc.pessoas.entity.Pais;
import br.com.cc.pessoas.filter.EstadoFilter;
import br.com.cc.pessoas.repository.EstadoRepository;
import br.com.cc.pessoas.repository.PaisRepository;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadoService {

    private final EstadoRepository estadoRepository;
    private final PaisRepository paisRepository;

    public EstadoService(EstadoRepository estadoRepository,
                         PaisRepository paisRepository) {
        this.estadoRepository = estadoRepository;
        this.paisRepository = paisRepository;
    }

    public List<EstadoDTO> listar(EstadoFilter filter) {
        return estadoRepository.filtrar(filter)
                .stream()
                .map(EstadoDTO::fromEstado)
                .toList();
    }

    // Busca por id
    public Estado findById(Long id) {
        return estadoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Estado não encontrado. Id: " + id
                ));
    }

    //Insert
    public Estado insert(@Valid EstadoCreateDTO dto) {
        Estado estado = new Estado();

        BeanUtils.copyProperties(dto, estado, "id");

        //Busco o pais
        Pais pais = paisRepository.findById(dto.paisId())
            .orElseThrow(() -> new ObjectNotFoundException(
                "País não encontrado. Id: " + dto.paisId()
        ));
        estado.setPais(pais);

        return estadoRepository.save(estado);
    }

    //Update
    public Estado update(Long id, EstadoUpdateDTO dto) {

        Estado estadoUpd = estadoRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Estado não cadastrado. Id: " + id));

        BeanUtils.copyProperties(dto, estadoUpd, "id");

        Pais pais = paisRepository.findById(dto.paisId())
            .orElseThrow(() -> new ObjectNotFoundException(
                "País não encontrado. Id: " + dto.paisId()
        ));
        estadoUpd.setPais(pais);

        return estadoRepository.save(estadoUpd);
    }

    // Delete
    public void delete(Long id) {
        Estado estado = findById(id);
        estadoRepository.delete(estado);
    }
}
