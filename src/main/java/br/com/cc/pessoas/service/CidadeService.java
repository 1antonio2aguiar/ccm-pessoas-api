package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.cidade.CidadeCreateDTO;
import br.com.cc.pessoas.dto.cidade.CidadeDTO;
import br.com.cc.pessoas.dto.cidade.CidadeUpdateDTO;
import br.com.cc.pessoas.entity.Cidade;
import br.com.cc.pessoas.entity.Estado;
import br.com.cc.pessoas.filter.CidadeFilter;
import br.com.cc.pessoas.repository.CidadeRepository;
import br.com.cc.pessoas.repository.EstadoRepository;
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
public class CidadeService {

    @Autowired private CidadeRepository cidadeRepository;
    @Autowired private EstadoRepository estadoRepository;

    public CidadeService(CidadeRepository cidadeRepository,
                         EstadoRepository estadoRepository) {
        this.cidadeRepository = cidadeRepository;
        this.estadoRepository = estadoRepository;
    }

    public List<CidadeDTO> listar(CidadeFilter filter) {
        return cidadeRepository.filtrar(filter)
                .stream()
                .map(CidadeDTO::fromCidade)
                .toList();
    }

    //Metodo filtrar paginacao
    public Page<CidadeDTO> pesquisar(CidadeFilter filter, Pageable pageable) {
        Page<Cidade> cidadePage = cidadeRepository.filtrar(filter, pageable);

        // Mapeia a lista de provas para uma lista de DadosListProvasRcd usando o método de fábrica
        List<CidadeDTO> cidadeDTOList = cidadePage.getContent().stream()
                .map(CidadeDTO::fromCidade)
                .collect(Collectors.toList());

        // Cria um novo Page<DadosListProvasRcd> com os dados mapeados
        return new PageImpl<>(cidadeDTOList, pageable, cidadePage.getTotalElements());
    }

    // Busca por id
    @Transactional(readOnly = true)
    public CidadeDTO findDtoById(Long id) {
        Cidade cidade = cidadeRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Cidade não encontrada. Id: " + id
                ));

        return CidadeDTO.fromCidade(cidade);
    }

    // Insert
    public Cidade insert(@Valid CidadeCreateDTO dto) {

        Cidade cidade = new Cidade();
        BeanUtils.copyProperties(dto, cidade, "id");

        Estado estado = estadoRepository.findById(dto.estadoId())
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Estado não encontrado. Id: " + dto.estadoId()
                ));

        cidade.setEstado(estado);

        return cidadeRepository.save(cidade);
    }

    // Update
    public Cidade update(Long id, CidadeUpdateDTO dto) {

        Cidade cidadeUpd = cidadeRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Cidade não cadastrada. Id: " + id
                ));

        BeanUtils.copyProperties(dto, cidadeUpd, "id");

        Estado estado = estadoRepository.findById(dto.estadoId())
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Estado não encontrado. Id: " + dto.estadoId()
                ));

        cidadeUpd.setEstado(estado);

        return cidadeRepository.save(cidadeUpd);
    }

    // Delete
    public void delete(Long id){
        Cidade cidadeDel = cidadeRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Cidade não cadastrada. Id: " + id));
        try {
            cidadeRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ObjectNotFoundException(id);
        } catch (DataIntegrityViolationException e){
            throw new DatabaseException(e.getMessage());
        }
    }
}
