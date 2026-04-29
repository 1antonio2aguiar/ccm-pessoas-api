package br.com.cc.pessoas.unificacao.pesService;

import br.com.cc.pessoas.unificacao.controle.ctrlEntity.PesCargaPessoasCtrl;
import br.com.cc.pessoas.unificacao.controle.ctrlEntity.PesCargaPessoasCtrlDto;
import br.com.cc.pessoas.unificacao.controle.ctrlEntity.PesCargaPessoasCtrlRepository;
import br.com.cc.pessoas.unificacao.pesEntity.PesPessoa;
import br.com.cc.pessoas.unificacao.pesFilter.PesPessoaFilter;
import br.com.cc.pessoas.unificacao.pesRepository.PesPessoaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public abstract class PesCargaPessoaUnicoBaseService {

    @Autowired
    protected PesPessoaRepository pesPessoaRepository;

    @Autowired
    protected PesCargaPessoasCtrlRepository cargaCtrlRepository;

    @PersistenceContext
    protected EntityManager manager;

    protected abstract String getFisicaJuridica();

    protected abstract Long getTipoEndereco();

    protected abstract void processarPessoa(PesPessoa pessoa);

    public Long iniciarCargaLote() {
        Long idControle = iniciarControle();
        executarCarga(idControle);
        return idControle;
    }

    @Transactional
    public void executarCarga(Long idControle) {
        try {
            marcarIniciado(idControle);

            int pagina = 0;
            int tamanho = 200;

            Page<PesPessoa> page;

            PesPessoaFilter filter = new PesPessoaFilter();
            filter.setFisicaJuridica(getFisicaJuridica());
            filter.setSomenteCpfUnico(true);
            filter.setSomenteNaoMigradas(true);

            do {
                page = pesPessoaRepository.filtrar(filter, PageRequest.of(pagina, tamanho));

                for (PesPessoa pessoa : page.getContent()) {
                    try {
                        processarPessoa(pessoa);
                        somarProcessado(idControle);
                    } catch (Exception e) {
                        somarErro(idControle, "Pessoa " + pessoa.getPessoa() + ": " + e.getMessage());
                    }
                }

                pagina++;

            } while (!page.isLast());

            marcarFinalizado(idControle);

        } catch (Exception e) {
            marcarErroGeral(idControle, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void processarPessoaUnica(Long pessoaId) {
        PesPessoa pessoa = pesPessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new RuntimeException("Pessoa não encontrada: " + pessoaId));

        processarPessoa(pessoa);
    }

    protected void inserirPessoa(Long idPessoa, PesPessoa pessoa, Long tipoPessoa) {
        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.PESSOAS
            (
                ID,
                TIPO_PESSOA_ID,
                NOME,
                DATA_CADASTRO,
                FISICA_JURIDICA,
                OBSERVACAO,
                SITUACAO_ID
            )
            values
            (
                :id,
                :tipoPessoa_id,
                :nome,
                :dataCadastro,
                :fisicaJuridica,
                :observacao,
                :situacao_id
            )
        """)
                .setParameter("id", idPessoa)
                .setParameter("tipoPessoa_id", tipoPessoa)
                .setParameter("nome", upper(pessoa.getNome()))
                .setParameter("dataCadastro", pessoa.getDataCadastro())
                .setParameter("fisicaJuridica", pessoa.getFisicaJuridica())
                .setParameter("observacao", pessoa.getObservacao())
                .setParameter("situacao_id", 1)
                .executeUpdate();
    }

    protected void inserirEnderecoPrincipal(Long idPessoa, PesPessoa pessoa) {
        EnderecoCarga endereco = null;

        endereco = resolverEnderecoFixo(pessoa);

        if (endereco == null) {
            endereco = buscarEnderecoSemCep(pessoa, 0);
        }

        if (endereco == null) {
            endereco = buscarEnderecoPorCep(pessoa, pessoa.getNumero());

            if (endereco == null) {
                endereco = buscarEnderecoSemCep(pessoa, 1);
            }
        }

        if (endereco == null) {
            return;
        }

        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.ENDERECOS
            (
                ID,
                PESSOA_ID,
                TIPO_ENDERECO,
                BAIRRO_ID,
                LOGRADOURO_ID,
                NUMERO,
                COMPLEMENTO,
                CEP_ID,
                BANCO,
                PRINCIPAL
            )
            values
            (
                SEQ_ENDERECOS.nextval,
                :pessoa_id,
                :tipoEndereco,
                :bairro_id,
                :logradouro_id,
                :numero,
                :complemento,
                :cep_id,
                :banco,
                :principal
            )
        """)
                .setParameter("pessoa_id", idPessoa)
                .setParameter("tipoEndereco", getTipoEndereco())
                .setParameter("bairro_id", endereco.bairroId)
                .setParameter("logradouro_id", endereco.logradouroId)
                .setParameter("numero", pessoa.getNumero())
                .setParameter("complemento", pessoa.getComplemento())
                .setParameter("cep_id", endereco.cepId)
                .setParameter("banco", "PESSOAS")
                .setParameter("principal", "S")
                .executeUpdate();
    }

    protected void inserirContatos(PesPessoa pessoa, Long idPessoa) {
        if (pessoa.getTelefone() != null && pessoa.getTelefone() != 0) {
            inserirContato(idPessoa, 0L, pessoa.getTelefone());
        }

        if (pessoa.getFax() != null) {
            inserirContato(idPessoa, 6L, pessoa.getFax());
        }

        if (pessoa.getEmail() != null) {
            inserirContato(idPessoa, 3L, pessoa.getEmail());
        }

        if (pessoa.getPaginaWeb() != null) {
            inserirContato(idPessoa, 4L, pessoa.getPaginaWeb());
        }

        if (pessoa.getRecado() != null && pessoa.getRecado() != 0) {
            inserirContato(idPessoa, 5L, pessoa.getRecado());
        }

        if (pessoa.getCelular() != null && pessoa.getCelular() != 0) {
            inserirContato(idPessoa, 1L, pessoa.getCelular());
        }
    }

    protected void inserirContato(Long idPessoa, Long tipoContato, Object contato) {
        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.CONTATOS
            (
                ID,
                PESSOA_ID,
                TIPO_CONTATO,
                CONTATO
            )
            values
            (
                SEQ_CONTATOS.nextval,
                :pessoa_id,
                :tipoContato,
                :contato
            )
        """)
                .setParameter("pessoa_id", idPessoa)
                .setParameter("tipoContato", tipoContato)
                .setParameter("contato", contato)
                .executeUpdate();
    }

    protected void inserirDocumentoSimples(Long idPessoa, Long tipoDocumento, String documento) {
        manager.createNativeQuery("""
            insert into DBO_CCM_PESSOAS.DOCUMENTOS
            (
                ID,
                PESSOA_ID,
                TIPO_DOCUMENTO,
                NUMERO_DOCUMENTO,
                ORGAO_EXPEDIDOR,
                DATA_EXPEDICAO
            )
            values
            (
                SEQ_DOCUMENTOS.nextval,
                :pessoa_id,
                :tipoDocumento,
                :numeroDocumento,
                :orgaoExpedidor,
                :dataExpedicao
            )
        """)
                .setParameter("pessoa_id", idPessoa)
                .setParameter("tipoDocumento", tipoDocumento)
                .setParameter("numeroDocumento", documento)
                .setParameter("orgaoExpedidor", null)
                .setParameter("dataExpedicao", null)
                .executeUpdate();
    }

    protected Long getNextVal(String sequence) {
        return ((Number) manager
                .createNativeQuery("select " + sequence + ".nextval from dual")
                .getSingleResult()).longValue();
    }

    protected Long getTipoPessoa(PesPessoa pessoa) {
        return pessoa.getPesTipoPessoa() != null
                ? pessoa.getPesTipoPessoa().getTipoPessoa()
                : 1L;
    }

    protected String upper(String valor) {
        return valor == null ? null : valor.toUpperCase();
    }

    protected Long getCidadeEndereco(PesPessoa pessoa) {
        if (pessoa.getCidade() != null) {
            return pessoa.getCidade();
        }
        return pessoa.getPesCidade() != null ? pessoa.getPesCidade().getCidade() : null;
    }

    protected Long getDistritoEndereco(PesPessoa pessoa) {
        if (pessoa.getDistrito() != null) {
            return pessoa.getDistrito();
        }
        return pessoa.getPesDistrito() != null ? pessoa.getPesDistrito().getDistrito() : null;
    }

    protected Long getLogradouroEndereco(PesPessoa pessoa) {
        if (pessoa.getLogradouro() != null) {
            return pessoa.getLogradouro();
        }
        return pessoa.getPesLogradouro() != null ? pessoa.getPesLogradouro().getLogradouro() : null;
    }

    protected Long getBairroEndereco(PesPessoa pessoa) {
        if (pessoa.getBairro() != null) {
            return pessoa.getBairro();
        }
        return pessoa.getPesBairro() != null ? pessoa.getPesBairro().getBairro() : null;
    }

    protected EnderecoCarga resolverEnderecoFixo(PesPessoa pessoa) {
        if (pessoa == null) {
            return null;
        }

        Long cidade = getCidadeEndereco(pessoa);
        Long bairro = getBairroEndereco(pessoa);
        Long distrito = getDistritoEndereco(pessoa);
        Long logradouro = getLogradouroEndereco(pessoa);

        String logradouroNome = pessoa.getLogradouroNome();
        String bairroNome = pessoa.getBairroNome();

        boolean enderecoEspecial =
                (Long.valueOf(1L).equals(cidade) || Long.valueOf(9999L).equals(cidade)) &&
                        (
                                Long.valueOf(8888L).equals(bairro) ||
                                        Long.valueOf(9999L).equals(bairro) ||
                                        Long.valueOf(8888L).equals(logradouro) ||
                                        Long.valueOf(9999L).equals(logradouro) ||
                                        Long.valueOf(8888L).equals(distrito) ||
                                        Long.valueOf(9999L).equals(distrito)
                        );

        if (!enderecoEspecial) {
            return null;
        }

        Long codigoCcm = buscarCodigoCcmDistrito(cidade, distrito);

        if (codigoCcm == null) {
            return null;
        }

        EnderecoCarga end = new EnderecoCarga();
        end.logradouroId = buscarLogradouroPorNome(codigoCcm, logradouroNome);
        end.bairroId = buscarBairroPorNome(codigoCcm, bairroNome);
        end.cepId = 1354012L;

        return end;
    }

    protected EnderecoCarga buscarEnderecoPorCep(PesPessoa pessoa, Long numero) {
        if (pessoa.getCep() == null || cepInvalido(pessoa.getCep())) {
            return null;
        }

        String cepLimpo = String.valueOf(pessoa.getCep()).trim();

        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = manager.createNativeQuery("""
                select id, bairro_id, logradouro_id
                  from (
                        select c.id,
                               c.bairro_id,
                               c.logradouro_id,
                               case
                                   when :numero is not null
                                    and c.numero_ini is not null
                                    and c.numero_fim is not null
                                    and :numero between c.numero_ini and c.numero_fim
                                   then 0
                                   else 1
                               end as ordem
                          from dbo_ccm_pessoas.ceps c
                         where trim(c.cep) = :cep
                         order by ordem, c.id
                       )
                 where rownum = 1
            """)
                    .setParameter("cep", cepLimpo)
                    .setParameter("numero", numero)
                    .getResultList();

            if (rows == null || rows.isEmpty()) {
                return null;
            }

            Object[] row = rows.get(0);

            EnderecoCarga end = new EnderecoCarga();
            end.cepId = row[0] != null ? ((Number) row[0]).longValue() : null;
            end.bairroId = row[1] != null ? ((Number) row[1]).longValue() : null;
            end.logradouroId = row[2] != null ? ((Number) row[2]).longValue() : null;

            return end;

        } catch (Exception e) {
            return null;
        }
    }

    protected EnderecoCarga buscarEnderecoSemCep(PesPessoa pessoa, Integer status) {
        if (pessoa == null) {
            return null;
        }

        if (!cepInvalido(pessoa.getCep()) && status == 0) {
            return null;
        }

        Long cidade = getCidadeEndereco(pessoa);
        Long distrito = getDistritoEndereco(pessoa);
        Long logradouro = getLogradouroEndereco(pessoa);
        Long bairro = getBairroEndereco(pessoa);

        if (cidade == null || distrito == null) {
            return null;
        }

        EnderecoCarga end = new EnderecoCarga();

        try {
            Object resultLogradouro = manager.createNativeQuery("""
                select l.codigo_ccm
                  from dbo_uni_pessoas.logradouros_unificado lu
                  join dbo_uni_pessoas.logradouros l
                    on l.cidade = lu.cidade_correios
                   and l.distrito = lu.distrito_correios
                   and l.logradouro = lu.logradouro_correios
                 where lu.cidade_pessoa = :cidade
                   and lu.distrito_pessoa = :distrito
                   and lu.logradouro_pessoa = :logradouro
            """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .setParameter("logradouro", logradouro)
                    .getSingleResult();

            end.logradouroId = resultLogradouro == null ? null : ((Number) resultLogradouro).longValue();

        } catch (Exception e) {
            end.logradouroId = null;
        }

        try {
            Object resultBairro = manager.createNativeQuery("""
                select l.codigo_ccm
                  from dbo_uni_pessoas.bairros_unificado lu
                  join dbo_uni_pessoas.bairros l
                    on l.cidade = lu.cidade_correios
                   and l.distrito = lu.distrito_correios
                   and l.bairro = lu.bairro_correios
                 where lu.cidade_pessoa = :cidade
                   and lu.distrito_pessoa = :distrito
                   and lu.bairro_pessoa = :bairro
            """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .setParameter("bairro", bairro)
                    .getSingleResult();

            end.bairroId = resultBairro == null ? null : ((Number) resultBairro).longValue();

        } catch (Exception e) {
            end.bairroId = null;
        }

        end.cepId = null;

        if (end.logradouroId == null && end.bairroId == null) {
            return null;
        }

        if (end.logradouroId != null && end.bairroId != null) {
            buscarCepPorBairroLogradouroNumero(pessoa, end);

            if (end.cepId == null || end.cepId == 0L) {
                buscarCepPorTabelaPessoas(cidade, distrito, logradouro, end);
            }
        }

        return end;
    }

    protected void buscarCepPorBairroLogradouroNumero(PesPessoa pessoa, EnderecoCarga end) {
        try {
            Long numero = pessoa.getNumero();
            if (numero == null || numero == 0L) {
                numero = 1L;
            }

            Object resultCep = manager.createNativeQuery("""
                select id
                  from (
                        select c.id,
                               case
                                   when :numero is not null
                                    and c.numero_ini is not null
                                    and c.numero_fim is not null
                                    and :numero between c.numero_ini and c.numero_fim
                                   then 0
                                   else 1
                               end as ordem
                          from dbo_ccm_pessoas.ceps c
                         where c.bairro_id = :bairroId
                           and c.logradouro_id = :logradouroId
                         order by ordem, c.id
                       )
                 where rownum = 1
            """)
                    .setParameter("bairroId", end.bairroId)
                    .setParameter("logradouroId", end.logradouroId)
                    .setParameter("numero", numero)
                    .getSingleResult();

            end.cepId = resultCep == null ? null : ((Number) resultCep).longValue();

        } catch (Exception e) {
            end.cepId = null;
        }
    }

    protected void buscarCepPorTabelaPessoas(Long cidade, Long distrito, Long logradouro, EnderecoCarga end) {
        try {
            Object resultCepPessoa = manager.createNativeQuery("""
                select cep
                  from (
                        select cep
                          from dbo_pessoas.ceps
                         where cidade = :cidade
                           and distrito = :distrito
                           and logradouro = :logradouro
                         order by cep
                       )
                 where rownum = 1
            """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .setParameter("logradouro", logradouro)
                    .getSingleResult();

            Long cepPessoa = resultCepPessoa == null ? null : ((Number) resultCepPessoa).longValue();

            if (cepPessoa == null) {
                end.cepId = null;
                return;
            }

            Object resultCepId = manager.createNativeQuery("""
                select id
                  from (
                        select id
                          from dbo_ccm_pessoas.ceps
                         where cep = :cep
                         order by id
                       )
                 where rownum = 1
            """)
                    .setParameter("cep", cepPessoa)
                    .getSingleResult();

            end.cepId = resultCepId == null ? null : ((Number) resultCepId).longValue();

        } catch (Exception e) {
            end.cepId = null;
        }
    }

    protected Long buscarCodigoCcmDistrito(Long cidade, Long distrito) {
        if (cidade == null || distrito == null) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
                select d.codigo_ccm
                  from dbo_uni_pessoas.distritos d,
                       dbo_uni_pessoas.distritos_unificado du
                 where d.cidade = du.cidade_correios
                   and d.distrito = du.distrito_correios
                   and du.cidade_pessoa = :cidade
                   and du.distrito_pessoa = :distrito
            """)
                    .setParameter("cidade", cidade)
                    .setParameter("distrito", distrito)
                    .getSingleResult();

            return result == null ? null : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    protected Long buscarLogradouroPorNome(Long distritoId, String logradouroNome) {
        if (distritoId == null || logradouroNome == null || logradouroNome.isBlank()) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
                select id
                  from dbo_ccm_pessoas.logradouros
                 where distrito_id = :distritoId
                   and upper(nome) = upper(:logradouroNome)
            """)
                    .setParameter("distritoId", distritoId)
                    .setParameter("logradouroNome", logradouroNome.trim())
                    .getSingleResult();

            return result == null ? null : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    protected Long buscarBairroPorNome(Long distritoId, String bairroNome) {
        if (distritoId == null || bairroNome == null || bairroNome.isBlank()) {
            return null;
        }

        try {
            Object result = manager.createNativeQuery("""
                select id
                  from dbo_ccm_pessoas.bairros
                 where distrito_id = :distritoId
                   and upper(nome) = upper(:bairroNome)
            """)
                    .setParameter("distritoId", distritoId)
                    .setParameter("bairroNome", bairroNome.trim())
                    .getSingleResult();

            return result == null ? null : ((Number) result).longValue();

        } catch (Exception e) {
            return null;
        }
    }

    protected void marcarIniciado(Long idControle) {
        PesCargaPessoasCtrl ctrl = cargaCtrlRepository.findById(idControle)
                .orElseThrow(() -> new RuntimeException("Controle não encontrado: " + idControle));

        ctrl.setStatus("PROCESSANDO");
        ctrl.setDataInicio(LocalDateTime.now());
        ctrl.setDataFim(null);
        ctrl.setMensagemErro(null);
        ctrl.setTotalProcessado(ctrl.getTotalProcessado() == null ? 0L : ctrl.getTotalProcessado());
        ctrl.setTotalErros(ctrl.getTotalErros() == null ? 0L : ctrl.getTotalErros());

        cargaCtrlRepository.save(ctrl);
    }

    protected void somarProcessado(Long idControle) {
        PesCargaPessoasCtrl ctrl = cargaCtrlRepository.findById(idControle)
                .orElseThrow(() -> new RuntimeException("Controle não encontrado: " + idControle));

        Long atual = ctrl.getTotalProcessado() == null ? 0L : ctrl.getTotalProcessado();
        ctrl.setTotalProcessado(atual + 1);

        cargaCtrlRepository.save(ctrl);
    }

    protected void somarErro(Long idControle, String mensagem) {
        PesCargaPessoasCtrl ctrl = cargaCtrlRepository.findById(idControle)
                .orElseThrow(() -> new RuntimeException("Controle não encontrado: " + idControle));

        Long atual = ctrl.getTotalErros() == null ? 0L : ctrl.getTotalErros();
        ctrl.setTotalErros(atual + 1);
        ctrl.setMensagemErro(mensagem);

        cargaCtrlRepository.save(ctrl);
    }

    protected void marcarFinalizado(Long idControle) {
        PesCargaPessoasCtrl ctrl = cargaCtrlRepository.findById(idControle)
                .orElseThrow(() -> new RuntimeException("Controle não encontrado: " + idControle));

        ctrl.setStatus("FINALIZADO");
        ctrl.setDataFim(LocalDateTime.now());

        cargaCtrlRepository.save(ctrl);
    }

    protected void marcarErroGeral(Long idControle, String mensagem) {
        PesCargaPessoasCtrl ctrl = cargaCtrlRepository.findById(idControle)
                .orElseThrow(() -> new RuntimeException("Controle não encontrado: " + idControle));

        ctrl.setStatus("ERRO");
        ctrl.setDataFim(LocalDateTime.now());
        ctrl.setMensagemErro(mensagem);

        cargaCtrlRepository.save(ctrl);
    }

    public Long iniciarControle() {
        Long idControle = getNextVal("DBO_CCM_PESSOAS.SEQ_CARGA_PESSOAS_CTRL");

        PesCargaPessoasCtrl ctrl = new PesCargaPessoasCtrl();
        ctrl.setId(idControle);
        ctrl.setStatus("INICIADO");
        ctrl.setTotalProcessado(0L);
        ctrl.setTotalErros(0L);

        cargaCtrlRepository.save(ctrl);

        return idControle;
    }

    public PesCargaPessoasCtrlDto buscarStatus(Long idControle) {
        PesCargaPessoasCtrl ctrl = cargaCtrlRepository.findById(idControle)
                .orElseThrow(() -> new RuntimeException("Controle não encontrado: " + idControle));

        return new PesCargaPessoasCtrlDto(
                ctrl.getId(),
                ctrl.getStatus(),
                ctrl.getTotalProcessado(),
                ctrl.getTotalErros(),
                ctrl.getMensagemErro()
        );
    }

    protected static final Set<Long> CEPS_INVALIDOS_FIXOS = Set.of(
            38100000L
    );

    protected boolean cepInvalido(Long cep) {
        if (cep == null) {
            return true;
        }

        return cep < 10000000L || cep > 99999999L || CEPS_INVALIDOS_FIXOS.contains(cep);
    }

    protected static class EnderecoCarga {
        protected Long bairroId;
        protected Long logradouroId;
        protected Long cepId;
    }
}
