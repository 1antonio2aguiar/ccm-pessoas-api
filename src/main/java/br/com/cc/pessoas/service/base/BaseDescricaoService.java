package br.com.cc.pessoas.service.base;

import br.com.cc.pessoas.dto.base.DescricaoDTO;
import br.com.cc.pessoas.service.exceptions.ObjectNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseDescricaoService<T> {

    protected final JpaRepository<T, Long> repository;

    protected BaseDescricaoService(JpaRepository<T, Long> repository) {
        this.repository = repository;
    }

    public List<DescricaoDTO> listar() {
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public DescricaoDTO findById(Long id) {
        T entity = repository.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Registro não encontrado. Id: " + id));
        return toDTO(entity);
    }

    public T insert(DescricaoDTO dto) {
        T entity = newEntity();
        BeanUtils.copyProperties(dto, entity, "id");
        return repository.save(entity);
    }

    public T update(Long id, DescricaoDTO dto) {
        T entity = repository.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Registro não encontrado. Id: " + id));

        BeanUtils.copyProperties(dto, entity, "id");
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    protected abstract DescricaoDTO toDTO(T entity);
    protected abstract T newEntity();
}
