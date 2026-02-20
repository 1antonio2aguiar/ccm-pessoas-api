package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.bairro.BairroCreateDTO;
import br.com.cc.pessoas.dto.bairro.BairroDTO;
import br.com.cc.pessoas.dto.bairro.BairroUpdateDTO;
import br.com.cc.pessoas.entity.Bairro;
import br.com.cc.pessoas.entity.Distrito;
import br.com.cc.pessoas.filter.BairroFilter;
import br.com.cc.pessoas.repository.BairroRepository;
import br.com.cc.pessoas.repository.DistritoRepository;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BairroService {

    @Autowired
    private BairroRepository bairroRepository;

    @Autowired
    private DistritoRepository distritoRepository;

    // LIST
    public List<BairroDTO> listar(BairroFilter filter) {
        return bairroRepository.filtrar(filter)
                .stream()
                .map(BairroDTO::fromBairro)
                .toList();
    }

    // PAGE
    public Page<BairroDTO> pesquisar(BairroFilter filter, Pageable pageable) {
        Page<Bairro> page = bairroRepository.filtrar(filter, pageable);
        List<BairroDTO> dtoList = page.getContent()
                .stream()
                .map(BairroDTO::fromBairro)
                .toList();
        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }

    // FIND BY ID
    @Transactional(readOnly = true)
    public BairroDTO findDtoById(Long id) {
        Bairro bairro = bairroRepository.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Bairro não encontrado. Id: " + id));
        return BairroDTO.fromBairro(bairro);
    }

    // INSERT
    public Bairro insert(BairroCreateDTO dto) {
        Bairro bairro = new Bairro();
        BeanUtils.copyProperties(dto, bairro);

        //Busco o distrito
        Distrito distrito = distritoRepository.findById(dto.distritoId()).get();
        bairro.setDistrito(distrito);

        bairro.setDistrito(distrito);
        return bairroRepository.save(bairro);
    }

    // update
    public Bairro update(Long id, BairroUpdateDTO dto){
        Bairro bairroUpd = bairroRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("Bairro não cadastrado. Id: " + id));

        BeanUtils.copyProperties(dto, bairroUpd, "id");

        System.err.println("bairro upd " + bairroUpd);
        return bairroRepository.save(bairroUpd);
    }

    // DELETE
    @Transactional
    public void delete(Long id) {
        Bairro bairro = bairroRepository.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Bairro não encontrado. Id: " + id));
        bairroRepository.delete(bairro);
    }
}