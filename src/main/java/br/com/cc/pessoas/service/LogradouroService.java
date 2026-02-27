package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.logradouro.*;
import br.com.cc.pessoas.filter.LogradouroFilter;
import br.com.cc.pessoas.repository.*;
import br.com.cc.pessoas.entity.Logradouro;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LogradouroService {

    @Autowired
    private LogradouroRepository logradouroRepository;

    @Autowired
    private DistritoRepository distritoRepository;

    @Autowired
    private TipoLogradouroRepository tipoLogradouroRepository;
    @Autowired
    private TituloPatenteRepository tituloPatenteRepository;

    public List<LogradouroDTO> listar(LogradouroFilter filter) {
        return logradouroRepository.filtrar(filter)
                .stream()
                .map(LogradouroDTO::fromLogradouro)
                .toList();
    }

    public Page<LogradouroDTO> pesquisar(LogradouroFilter filter, Pageable pageable) {
        Page<Logradouro> page = logradouroRepository.filtrar(filter, pageable);
        return page.map(LogradouroDTO::fromLogradouro);
    }

    @Transactional(readOnly = true)
    public LogradouroDTO findDtoById(Long id) {
        Logradouro logradouro = logradouroRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Logradouro não encontrado. Id: " + id));
        return LogradouroDTO.fromLogradouro(logradouro);
    }

    public Logradouro insert(LogradouroCreateDTO dto) {
        Logradouro logradouro = new Logradouro();
        BeanUtils.copyProperties(dto, logradouro);

        logradouro.setDistrito(
                distritoRepository.findById(dto.distritoId()).get()
        );

        logradouro.setTipoLogradouro(
                tipoLogradouroRepository.findById(dto.tipoLogradouroId()).get()
        );

        logradouro.setTituloPatente(
                (dto.getTituloPatente() != null && !dto.getTituloPatente().isBlank())
                        ? dto.getTituloPatente().trim().toUpperCase()
                        : null
        );

        return logradouroRepository.save(logradouro);
    }

    public Logradouro update(Long id, LogradouroUpdateDTO dto) {
        Logradouro logradouro = logradouroRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Logradouro não encontrado. Id: " + id));

        BeanUtils.copyProperties(dto, logradouro, "id");

        logradouro.setTipoLogradouro(
                tipoLogradouroRepository.findById(dto.tipoLogradouroId()).get()
        );

        return logradouroRepository.save(logradouro);
    }

    @Transactional
    public void delete(Long id) {
        Logradouro logradouro = logradouroRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Logradouro não encontrado. Id: " + id));
        logradouroRepository.delete(logradouro);
    }
}
