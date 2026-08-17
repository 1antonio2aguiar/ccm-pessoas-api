package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.estabelecimento.EstabelecimentoCreateDTO;
import br.com.cc.pessoas.dto.estabelecimento.EstabelecimentoDTO;
import br.com.cc.pessoas.dto.estabelecimento.EstabelecimentoUpdateDTO;
import br.com.cc.pessoas.entity.DadosPessoaJuridica;
import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.filter.PessoaFilter;
import br.com.cc.pessoas.repository.EstabelecimentoRepository;
import br.com.cc.pessoas.repository.PessoaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class EstabelecimentoService {

    @Autowired
    private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired
    private PessoaRepository pessoaRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<EstabelecimentoDTO> listar(
            PessoaFilter filter) {

        return estabelecimentoRepository.filtrar(filter);
    }

    @Transactional(readOnly = true)
    public Page<EstabelecimentoDTO> pesquisar(
            PessoaFilter filter,
            Pageable pageable) {

        return estabelecimentoRepository.filtrar(
                filter,
                pageable
        );
    }
    @Transactional(readOnly = true)
    public EstabelecimentoDTO findById(Long id) {

        return estabelecimentoRepository.findById(id)
        .map(EstabelecimentoDTO::fromEntity)
        .orElseThrow(() ->
                new EntityNotFoundException(
                        "Estabelecimento não encontrado. ID: " + id
                )
        );
    }
    @Transactional
    public EstabelecimentoDTO insert(
            EstabelecimentoCreateDTO dto) {

        Pessoa pessoa = pessoaRepository
                .findById(dto.pessoaId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Proprietário não encontrado. ID: "
                                        + dto.pessoaId()
                        )
                );

        String cnpj = normalizarCnpj(dto.cnpj());

        validarCnpj(cnpj);

        if (estabelecimentoRepository.existsByCnpj(cnpj)) {
            throw new IllegalArgumentException(
                    "Já existe um estabelecimento cadastrado com o CNPJ informado."
            );
        }

        DadosPessoaJuridica estabelecimento =
                new DadosPessoaJuridica();

        estabelecimento.setPessoa(pessoa);

        preencherDados(
                estabelecimento,
                cnpj,
                dto.nome(),
                dto.estabelecimento(),
                dto.nomeFantasia(),
                dto.objetoSocial(),
                dto.microEmpresa(),
                dto.conjuge(),
                dto.tipoEmpresa()
        );

        DadosPessoaJuridica estabelecimentoSalvo =
                estabelecimentoRepository.saveAndFlush(
                        estabelecimento
                );

        return EstabelecimentoDTO.fromEntity(
                estabelecimentoSalvo
        );
    }
    @Transactional
    public EstabelecimentoDTO update(
            Long id,
            EstabelecimentoUpdateDTO dto) {

        DadosPessoaJuridica estabelecimento =
                estabelecimentoRepository.findById(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Estabelecimento não encontrado. ID: "
                                                + id
                                )
                        );

        String cnpj = StringUtils.hasText(dto.cnpj())
                ? normalizarCnpj(dto.cnpj())
                : estabelecimento.getCnpj();

        validarCnpj(cnpj);

        if (estabelecimentoRepository
                .existsByCnpjAndIdNot(cnpj, id)) {

            throw new IllegalArgumentException(
                    "Já existe outro estabelecimento cadastrado com o CNPJ informado."
            );
        }

        String nome = StringUtils.hasText(dto.nome())
                ? dto.nome()
                : estabelecimento.getNome();

        preencherDados(
                estabelecimento,
                cnpj,
                nome,
                dto.estabelecimento(),
                dto.nomeFantasia(),
                dto.objetoSocial(),
                dto.microEmpresa(),
                dto.conjuge(),
                dto.tipoEmpresa()
        );

        DadosPessoaJuridica estabelecimentoAtualizado =
                estabelecimentoRepository.saveAndFlush(
                        estabelecimento
                );

        return EstabelecimentoDTO.fromEntity(
                estabelecimentoAtualizado
        );
    }
    @Transactional
    public void delete(Long id) {

        DadosPessoaJuridica estabelecimento =
                estabelecimentoRepository.findById(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Estabelecimento não encontrado. ID: "
                                                + id
                                )
                        );

        excluirVinculosDoEstabelecimento(id);

        estabelecimentoRepository.delete(estabelecimento);
        estabelecimentoRepository.flush();
    }
    private void excluirVinculosDoEstabelecimento(
            Long dadosPessoaJuridicaId) {

        entityManager.createNativeQuery("""
            DELETE FROM DBO_CCM_PESSOAS.ENDERECOS
             WHERE DADOS_PJ_ID = :dadosPessoaJuridicaId
            """)
                .setParameter(
                        "dadosPessoaJuridicaId",
                        dadosPessoaJuridicaId
                )
                .executeUpdate();

        entityManager.createNativeQuery("""
            DELETE FROM DBO_CCM_PESSOAS.CONTATOS
             WHERE DADOS_PJ_ID = :dadosPessoaJuridicaId
            """)
                .setParameter(
                        "dadosPessoaJuridicaId",
                        dadosPessoaJuridicaId
                )
                .executeUpdate();

        entityManager.createNativeQuery("""
            DELETE FROM DBO_CCM_PESSOAS.DOCUMENTOS
             WHERE DADOS_PJ_ID = :dadosPessoaJuridicaId
            """)
                .setParameter(
                        "dadosPessoaJuridicaId",
                        dadosPessoaJuridicaId
                )
                .executeUpdate();
    }
    private void preencherDados(
            DadosPessoaJuridica estabelecimento,
            String cnpj,
            String nome,
            Long numeroEstabelecimento,
            String nomeFantasia,
            String objetoSocial,
            String microEmpresa,
            String conjuge,
            Long tipoEmpresa) {

        estabelecimento.setCnpj(cnpj);

        estabelecimento.setNome(
                StringUtils.hasText(nome)
                        ? nome.trim().toUpperCase()
                        : null
        );

        estabelecimento.setEstabelecimento(
                numeroEstabelecimento
        );

        estabelecimento.setNomeFantasia(
                normalizarTexto(nomeFantasia)
        );

        estabelecimento.setObjetoSocial(
                normalizarTexto(objetoSocial)
        );

        estabelecimento.setMicroEmpresa(
                StringUtils.hasText(microEmpresa)
                        ? microEmpresa.trim().toUpperCase()
                        : null
        );

        estabelecimento.setConjuge(
                normalizarTexto(conjuge)
        );

        estabelecimento.setTipoEmpresa(
                tipoEmpresa
        );
    }

    private String normalizarCnpj(String cnpj) {

        return cnpj == null
                ? ""
                : cnpj.replaceAll("\\D", "");
    }

    private String normalizarTexto(String valor) {

        return StringUtils.hasText(valor)
                ? valor.trim().toUpperCase()
                : null;
    }

    private void validarCnpj(String cnpj) {

        if (cnpj.length() != 14) {
            throw new IllegalArgumentException(
                    "O CNPJ deve possuir 14 dígitos."
            );
        }
    }
}
