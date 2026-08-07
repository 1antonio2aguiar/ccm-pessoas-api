package br.com.cc.pessoas.unificacao.pessoa.pesService;

import org.springframework.stereotype.Service;

@Service
public class PesPessoaCpfDuplicadoService
        extends PesCargaPessoaCpfDuplicadoBaseService {

    @Override
    protected void insertDadosEspecificos(
            Long idPessoa,
            DadosGrupo dadosGrupo) {

        inserirDadosPf(
                idPessoa,
                dadosGrupo,
                dadosGrupo.cpfCnpjPrincipal,
                dadosGrupo.estadoCivilDestino,
                dadosGrupo.cidadeNascimentoCcm
        );
    }
}