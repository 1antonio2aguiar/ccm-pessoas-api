package br.com.cc.pessoas.repository.endereco;

import br.com.cc.pessoas.entity.Endereco;
import com.pesoas.api.filter.enderecos.EnderecoFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnderecoRepositoryQuery {
    public Page<Endereco> filtrar(EnderecoFilter enderecoFilter, Pageable pageable);

    public List<Endereco> filtrar(EnderecoFilter Filter);
}
