package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.pessoa.*;
import br.com.cc.pessoas.entity.*;
import br.com.cc.pessoas.entity.enuns.EstadoCivil;
import br.com.cc.pessoas.entity.enuns.TipoDocumento;
import br.com.cc.pessoas.filter.PessoaFilter;
import br.com.cc.pessoas.repository.PessoaRepository;
import br.com.cc.pessoas.repository.TipoPessoaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PessoaService {

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private TipoPessoaRepository tipoPessoaRepository; // usado só para validar tipoPessoaId

    // ===============================
    // LISTAR
    // ===============================
    @Transactional(readOnly = true)
    public List<PessoaDTO> listar(PessoaFilter filter) {
        return pessoaRepository.filtrar(filter)
                .stream()
                .map(PessoaDTO::fromPessoa)
                .toList();
    }

    // ===============================
    // PESQUISAR
    // ===============================
    @Transactional(readOnly = true)
    public Page<PessoaDTO> pesquisar(PessoaFilter filter, Pageable pageable) {
        Page<Pessoa> page = pessoaRepository.filtrar(filter, pageable);
        return page.map(PessoaDTO::fromPessoa);
    }

    // ===============================
    // FIND BY ID
    // ===============================
    @Transactional(readOnly = true)
    public PessoaDTO findDtoById(Long id) {
        return pessoaRepository.findById(id)
                .map(PessoaDTO::fromPessoa)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada"));
    }

    // ===============================
    // INSERT
    // ===============================
    @Transactional
    public Pessoa insert(PessoaCreateDTO dto) {
        validarTipoPessoa(dto.tipoPessoaId());

        // 🔥 Agora você NÃO instancia Pessoa (é abstract).
        // Você instancia a SUBCLASSE correta.
        Pessoa pessoa;

        if ("F".equalsIgnoreCase(dto.fisicaJuridica())) {
            pessoa = buildPessoaFisica(dto);
        } else if ("J".equalsIgnoreCase(dto.fisicaJuridica())) {
            pessoa = buildPessoaJuridica(dto);
        } else {
            throw new IllegalArgumentException("fisicaJuridica deve ser 'F' ou 'J'");
        }

        return pessoaRepository.save(pessoa);
    }

    private DadosPessoaFisica buildPessoaFisica(PessoaCreateDTO dto) {
        DadosPessoaFisica pf = new DadosPessoaFisica();

        // base (PESSOAS)
        pf.setNome(dto.nome());
        pf.setFisicaJuridica("F");
        pf.setTipoPessoaId(dto.tipoPessoaId());
        pf.setSituacaoId(dto.situacaoId());
        pf.setDataCadastro(dto.dataCadastro() != null ? dto.dataCadastro() : LocalDateTime.now());
        pf.setObservacao(dto.observacao());

        // específica (DADOS_PF)
        if (dto.dadosPessoaFisica() != null) {
            DadosPessoaFisicaDTO pfDTO = dto.dadosPessoaFisica();

            if (StringUtils.hasText(pfDTO.cpf())) pf.setCpf(pfDTO.cpf().trim());
            pf.setNomeSocial(pfDTO.nomeSocial());
            pf.setRaca(pfDTO.raca());
            pf.setEtnia(pfDTO.etnia());
            pf.setCor(pfDTO.cor());
            pf.setRecebeBf(pfDTO.recebeBf());
            pf.setCartaoSus(pfDTO.cartaoSus());
            pf.setSexo(pfDTO.sexo());

            // set o enum
            pf.setEstadoCivil(EstadoCivil.toEstadoCivilEnum(dto.dadosPessoaFisica().estadoCivil()));

            pf.setLocalNascimentoId(pfDTO.localNascimentoId());
            pf.setMae(pfDTO.mae());
            pf.setPai(pfDTO.pai());
            pf.setDataNascimento(pfDTO.dataNascimento());

            // ⚠️ Se você manteve "observacao" também na tabela DADOS_PF, descomente:
            // pf.setObservacao(pfDTO.observacao());
        }

        return pf;
    }

    private DadosPessoaJuridica buildPessoaJuridica(PessoaCreateDTO dto) {

        DadosPessoaJuridica pj = new DadosPessoaJuridica();

        // base (PESSOAS)
        pj.setNome(dto.nome());
        pj.setFisicaJuridica("J");
        pj.setTipoPessoaId(dto.tipoPessoaId());
        pj.setSituacaoId(dto.situacaoId());
        pj.setDataCadastro(dto.dataCadastro() != null ? dto.dataCadastro() : LocalDateTime.now());
        pj.setObservacao(dto.observacao());

        // específica (DADOS_PJ)
        if (dto.dadosPessoaJuridica() != null) {
            DadosPessoaJuridicaDTO pjDTO = dto.dadosPessoaJuridica();

            if (StringUtils.hasText(pjDTO.cnpj())) pj.setCnpj(pjDTO.cnpj().trim());
            pj.setNomeFantasia(pjDTO.nomeFantasia());
            pj.setObjetoSocial(pjDTO.objetoSocial());
            pj.setMicroEmpresa(pjDTO.microEmpresa());
            pj.setConjuge(pjDTO.conjuge());
            pj.setTipoEmpresa(pjDTO.tipoEmpresa());
        }

        return pj;
    }

    // ===============================
    // UPDATE
    // ===============================
    @Transactional
    public PessoaDTO update(Long id, PessoaUpdateDTO dto){

        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada"));

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

        // Atualiza PF (somente se for PF)
        if (pessoa instanceof DadosPessoaFisica pf && dto.dadosPessoaFisica() != null) {
            DadosPessoaFisicaDTO pfDTO = dto.dadosPessoaFisica();

            if (StringUtils.hasText(pfDTO.cpf())) pf.setCpf(pfDTO.cpf().trim());

            pf.setNomeSocial(pfDTO.nomeSocial());
            pf.setRaca(pfDTO.raca());
            pf.setEtnia(pfDTO.etnia());
            pf.setCor(pfDTO.cor());
            pf.setRecebeBf(pfDTO.recebeBf());
            pf.setCartaoSus(pfDTO.cartaoSus());
            pf.setSexo(pfDTO.sexo());
            pf.setEstadoCivil(EstadoCivil.toEstadoCivilEnum(dto.dadosPessoaFisica().estadoCivil()));
            pf.setLocalNascimentoId(pfDTO.localNascimentoId());
            pf.setMae(pfDTO.mae());
            pf.setPai(pfDTO.pai());
            pf.setDataNascimento(pfDTO.dataNascimento());
        }

        // Atualiza PJ (somente se for PJ)
        if (pessoa instanceof DadosPessoaJuridica pj && dto.dadosPessoaJuridica() != null) {
            DadosPessoaJuridicaDTO pjDTO = dto.dadosPessoaJuridica();

            // CNPJ normalmente não muda, mas se quiser permitir:
            if (StringUtils.hasText(pjDTO.cnpj())) pj.setCnpj(pjDTO.cnpj().trim());

            pj.setNomeFantasia(pjDTO.nomeFantasia());
            pj.setObjetoSocial(pjDTO.objetoSocial());
            pj.setMicroEmpresa(pjDTO.microEmpresa());
            pj.setConjuge(pjDTO.conjuge());
            pj.setTipoEmpresa(pjDTO.tipoEmpresa());
        }

        Pessoa salva = pessoaRepository.save(pessoa);
        return PessoaDTO.fromPessoa(salva);
    }

    // ===============================
    // DELETE
    // ===============================
    @Transactional
    public void delete(Long id) {
        if (!pessoaRepository.existsById(id)) {
            throw new EntityNotFoundException("Pessoa não encontrada");
        }
        pessoaRepository.deleteById(id);
    }

    private void validarTipoPessoa(Long tipoPessoaId) {
        if (!tipoPessoaRepository.existsById(tipoPessoaId)) {
            throw new EntityNotFoundException("TipoPessoa não encontrado");
        }
    }
}
