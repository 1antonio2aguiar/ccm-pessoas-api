package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa.CadUnicoPessoaCreateDTO;
import br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa.CadUnicoPessoaDTO;
import br.com.cc.pessoas.unificacao.pesEntity.CadUnicoPessoa;
import br.com.cc.pessoas.unificacao.pesFilter.CadUnicoPessoaFilter;
import br.com.cc.pessoas.unificacao.pesRepository.CadUnicoPessoaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CadUnicoPessoaService {
    @Autowired private CadUnicoPessoaRepository repository;
    public Page<CadUnicoPessoaDTO> filtrar(CadUnicoPessoaFilter filter, Pageable pageable) {
        return repository.filtrar(filter, pageable)
                .map(CadUnicoPessoaDTO::fromEntity);
    }

    public List<CadUnicoPessoaDTO> filtrar(CadUnicoPessoaFilter filter) {
        return repository.filtrar(filter)
                .stream()
                .map(CadUnicoPessoaDTO::fromEntity)
                .toList();
    }

    public CadUnicoPessoaDTO salvar(CadUnicoPessoaCreateDTO dto) {
        CadUnicoPessoa entity = dto.toEntity();

        entity = repository.save(entity);

        return CadUnicoPessoaDTO.fromEntity(entity);
    }
}