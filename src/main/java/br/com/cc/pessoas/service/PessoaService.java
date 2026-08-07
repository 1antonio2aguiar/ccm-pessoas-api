package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.pessoa.*;
import br.com.cc.pessoas.entity.DadosPessoaFisica;
import br.com.cc.pessoas.entity.DadosPessoaJuridica;
import br.com.cc.pessoas.entity.Pessoa;
import br.com.cc.pessoas.entity.enuns.EstadoCivil;
import br.com.cc.pessoas.filter.PessoaFilter;

import br.com.cc.pessoas.repository.PessoaRepository;
import br.com.cc.pessoas.repository.PessoaFisicaRepository;

import br.com.cc.pessoas.repository.TipoPessoaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PessoaService {

    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private PessoaFisicaRepository pessoaFisicaRepository;

    @Autowired
    private TipoPessoaRepository tipoPessoaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ===============================
    // LISTAR
    // ===============================
    @Transactional(readOnly = true)
    public List<PessoaDTO> listar(PessoaFilter filter) {
        return pessoaFisicaRepository.filtrar(filter);
    }

    // ===============================
    // PESQUISAR
    // ===============================
    @Transactional(readOnly = true)
    public Page<PessoaDTO> pesquisar(
            PessoaFilter filter,
            Pageable pageable) {

        return pessoaFisicaRepository.filtrar(filter, pageable);
    }

    // ===============================
    // BUSCAR POR ID
    // ===============================
    @Transactional(readOnly = true)
    public PessoaDTO findDtoById(Long id) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Pessoa não encontrada"));

        return PessoaDTO.fromPessoa(pessoa);
    }

    public PessoaCpfCnpjCadUnicoDTO existeCpfCnpjNoCadUnico(
            Long cpfCnpj,
            String fisicaJuridica) {

        //System.err.println("Esta PASSANDO AQUI >>>>>>>>>>>>>>>>>>>>> " + cpfCnpj);

        return pessoaRepository.existeCpfCnpjNoCadUnico(
                cpfCnpj,
                fisicaJuridica
        );
    }

    // ===============================
    // INSERT
    // ===============================
    @Transactional
    public Pessoa insert(PessoaCreateDTO dto) {

        validarTipoPessoa(dto.tipoPessoaId());
        validarFisicaJuridica(dto.fisicaJuridica());

        Pessoa pessoa = new Pessoa();

        pessoa.setNome(dto.nome().trim());
        pessoa.setFisicaJuridica(dto.fisicaJuridica().trim().toUpperCase());
        pessoa.setTipoPessoaId(dto.tipoPessoaId());
        pessoa.setSituacaoId(dto.situacaoId());
        pessoa.setDataCadastro(
                dto.dataCadastro() != null
                        ? dto.dataCadastro()
                        : LocalDateTime.now()
        );
        pessoa.setObservacao(dto.observacao());

        pessoa = pessoaRepository.saveAndFlush(pessoa);

        if (dto.dadosPessoaFisica() != null) {
            DadosPessoaFisica pf = criarDadosPessoaFisica(
                    pessoa,
                    dto.dadosPessoaFisica()
            );

            entityManager.persist(pf);
            pessoa.setDadosPessoaFisica(pf);
        }

        List<DadosPessoaJuridica> empresas = new ArrayList<>();

        if (dto.dadosPessoasJuridicas() != null) {
            for (DadosPessoaJuridicaDTO pjDTO
                    : dto.dadosPessoasJuridicas()) {

                DadosPessoaJuridica pj =
                        criarDadosPessoaJuridica(pessoa, pjDTO);

                entityManager.persist(pj);
                empresas.add(pj);
            }
        }

        pessoa.setDadosPessoasJuridicas(empresas);

        entityManager.flush();

        return pessoa;
    }

    // ===============================
    // UPDATE
    // ===============================
    @Transactional
    public PessoaDTO update(Long id, PessoaUpdateDTO dto) {

        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Pessoa não encontrada"));

        atualizarDadosGerais(pessoa, dto);
        atualizarDadosPessoaFisica(pessoa, dto.dadosPessoaFisica());

        if (dto.dadosPessoasJuridicas() != null) {
            sincronizarDadosPessoasJuridicas(
                    pessoa,
                    dto.dadosPessoasJuridicas()
            );
        }

        pessoaRepository.save(pessoa);
        entityManager.flush();

        return PessoaDTO.fromPessoa(pessoa);
    }

    private void atualizarDadosGerais(
            Pessoa pessoa,
            PessoaUpdateDTO dto) {

        if (StringUtils.hasText(dto.nome())) {
            pessoa.setNome(dto.nome().trim());
        }

        if (dto.tipoPessoaId() != null) {
            validarTipoPessoa(dto.tipoPessoaId());
            pessoa.setTipoPessoaId(dto.tipoPessoaId());
        }

        if (dto.situacaoId() != null) {
            pessoa.setSituacaoId(dto.situacaoId());
        }

        if (dto.dataCadastro() != null) {
            pessoa.setDataCadastro(dto.dataCadastro());
        }

        if (dto.observacao() != null) {
            pessoa.setObservacao(dto.observacao());
        }
    }

    // ===============================
    // DADOS PF - 1:1
    // ===============================
    private void atualizarDadosPessoaFisica(
            Pessoa pessoa,
            DadosPessoaFisicaDTO dto) {

        if (dto == null) {
            return;
        }

        DadosPessoaFisica pf = pessoa.getDadosPessoaFisica();

        if (pf == null) {
            pf = criarDadosPessoaFisica(pessoa, dto);
            entityManager.persist(pf);
            pessoa.setDadosPessoaFisica(pf);
            return;
        }

        preencherDadosPessoaFisica(pf, dto);
    }

    private DadosPessoaFisica criarDadosPessoaFisica(
            Pessoa pessoa,
            DadosPessoaFisicaDTO dto) {

        DadosPessoaFisica pf = new DadosPessoaFisica();

        pf.setPessoa(pessoa);
        preencherDadosPessoaFisica(pf, dto);

        return pf;
    }

    private void preencherDadosPessoaFisica(
            DadosPessoaFisica pf,
            DadosPessoaFisicaDTO dto) {

        if (StringUtils.hasText(dto.cpf())) {
            pf.setCpf(somenteNumeros(dto.cpf()));
        }

        pf.setNomeSocial(dto.nomeSocial());
        pf.setRaca(dto.raca());
        pf.setEtnia(dto.etnia());
        pf.setCor(dto.cor());
        pf.setRecebeBf(dto.recebeBf());
        pf.setCartaoSus(dto.cartaoSus());
        pf.setSexo(dto.sexo());

        pf.setEstadoCivil(
                EstadoCivil.toEstadoCivilEnum(dto.estadoCivil())
        );

        pf.setLocalNascimentoId(dto.localNascimentoId());
        pf.setMae(dto.mae());
        pf.setPai(dto.pai());
        pf.setDataNascimento(dto.dataNascimento());
    }

    // ===============================
    // DADOS PJ - 1:N
    // ===============================
    private void sincronizarDadosPessoasJuridicas(
            Pessoa pessoa,
            List<DadosPessoaJuridicaDTO> dtos) {

        List<DadosPessoaJuridica> existentes =
                entityManager.createQuery("""
                        select pj
                          from DadosPessoaJuridica pj
                         where pj.pessoa.id = :pessoaId
                        """, DadosPessoaJuridica.class)
                        .setParameter("pessoaId", pessoa.getId())
                        .getResultList();

        Map<Long, DadosPessoaJuridica> existentesPorId =
                new HashMap<>();

        for (DadosPessoaJuridica pj : existentes) {
            existentesPorId.put(pj.getId(), pj);
        }

        List<DadosPessoaJuridica> atualizadas =
                new ArrayList<>();

        for (DadosPessoaJuridicaDTO dto : dtos) {

            DadosPessoaJuridica pj;

            if (dto.id() == null) {
                pj = criarDadosPessoaJuridica(pessoa, dto);
                entityManager.persist(pj);
            } else {
                pj = existentesPorId.remove(dto.id());

                if (pj == null) {
                    throw new IllegalArgumentException(
                            "CNPJ não pertence à pessoa informada: "
                                    + dto.id()
                    );
                }

                preencherDadosPessoaJuridica(pj, dto);
            }

            atualizadas.add(pj);
        }

        for (DadosPessoaJuridica removida
                : existentesPorId.values()) {

            deletarVinculosDoEstabelecimento(removida.getId());

            entityManager.remove(removida);
        }

        pessoa.setDadosPessoasJuridicas(atualizadas);
    }

    private DadosPessoaJuridica criarDadosPessoaJuridica(
            Pessoa pessoa,
            DadosPessoaJuridicaDTO dto) {

        DadosPessoaJuridica pj =
                new DadosPessoaJuridica();

        pj.setPessoa(pessoa);
        preencherDadosPessoaJuridica(pj, dto);

        return pj;
    }

    private void preencherDadosPessoaJuridica(
            DadosPessoaJuridica pj,
            DadosPessoaJuridicaDTO dto) {

        if (StringUtils.hasText(dto.cnpj())) {
            pj.setCnpj(somenteNumeros(dto.cnpj()));
        }

        pj.setNome(dto.nome());
        pj.setEstabelecimento(dto.estabelecimento());
        pj.setNomeFantasia(dto.nomeFantasia());
        pj.setObjetoSocial(dto.objetoSocial());
        pj.setMicroEmpresa(dto.microEmpresa());
        pj.setConjuge(dto.conjuge());
        pj.setMesEnvioSicom(dto.mesEnvioSicom());
        pj.setAnoEnvioSicom(dto.anoEnvioSicom());
        pj.setTipoEmpresa(dto.tipoEmpresa());
    }

    // ===============================
    // DELETE
    // ===============================
    @Transactional
    public void delete(Long id) {

        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Pessoa não encontrada"));

        deletarVinculosDaPessoa(id);

        pessoaRepository.delete(pessoa);
        pessoaRepository.flush();
    }

    private void deletarVinculosDaPessoa(Long pessoaId) {

        entityManager.createNativeQuery("""
                DELETE FROM DBO_CCM_PESSOAS.CAD_UNICO_PESSOA
                 WHERE PESSOAS_CD_UNICO = :pessoaId
                """)
                .setParameter("pessoaId", pessoaId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM DBO_CCM_PESSOAS.ENDERECOS
                 WHERE PESSOA_ID = :pessoaId
                """)
                .setParameter("pessoaId", pessoaId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM DBO_CCM_PESSOAS.DOCUMENTOS
                 WHERE PESSOA_ID = :pessoaId
                """)
                .setParameter("pessoaId", pessoaId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM DBO_CCM_PESSOAS.CONTATOS
                 WHERE PESSOA_ID = :pessoaId
                """)
                .setParameter("pessoaId", pessoaId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM DBO_CCM_PESSOAS.DADOS_PJ
                 WHERE PESSOA_ID = :pessoaId
                """)
                .setParameter("pessoaId", pessoaId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM DBO_CCM_PESSOAS.DADOS_PF
                 WHERE ID = :pessoaId
                """)
                .setParameter("pessoaId", pessoaId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM DBO_CCM_PESSOAS.PESSOAS
                 WHERE ID = :pessoaId
                """)
                .setParameter("pessoaId", pessoaId)
                .executeUpdate();
    }

    // ===============================
    // VALIDAÇÕES
    // ===============================
    private void validarTipoPessoa(Long tipoPessoaId) {
        if (!tipoPessoaRepository.existsById(tipoPessoaId)) {
            throw new EntityNotFoundException(
                    "TipoPessoa não encontrado"
            );
        }
    }

    private void validarFisicaJuridica(String fisicaJuridica) {
        if (!StringUtils.hasText(fisicaJuridica)
                || (!"F".equalsIgnoreCase(fisicaJuridica)
                && !"J".equalsIgnoreCase(fisicaJuridica))) {

            throw new IllegalArgumentException(
                    "fisicaJuridica deve ser 'F' ou 'J'"
            );
        }
    }

    private String somenteNumeros(String valor) {
        return valor == null
                ? null
                : valor.replaceAll("\\D", "");
    }

    private void deletarVinculosDoEstabelecimento(
            Long dadosPessoaJuridicaId) {

        if (dadosPessoaJuridicaId == null) {
            return;
        }

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
    }
}