package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.pesDto.cadUnicoPessoa.CadUnicoPessoaOrigemDTO;
import br.com.cc.pessoas.unificacao.pesRepository.CadUnicoPessoaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CadUnicoPessoaService {

    private final CadUnicoPessoaRepository repository;

    public List<CadUnicoPessoaOrigemDTO> buscarOrigens(Long pessoasCdUnico) {
        if (pessoasCdUnico == null) {
            throw new RuntimeException("Código da pessoa unificada não informado.");
        }

        return repository.buscarOrigens(pessoasCdUnico);
    }
}
