package br.com.cc.pessoas.service;

import br.com.cc.pessoas.dto.imovel.ImovelDTO;
import br.com.cc.pessoas.dto.imovel.ImovelDividaDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImovelService {
    private final DataSource dataSource;

    public ImovelService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Transactional(readOnly = true)
    public List<ImovelDTO> listarPorPessoa(Long pessoasCdUnico, Integer anoInicial,
                                          Integer anoFinal, LocalDate dataCorrecao) {
        if (pessoasCdUnico == null || pessoasCdUnico <= 0) {
            throw new IllegalArgumentException("Identificador da pessoa inválido");
        }
        if (anoInicial == null || anoFinal == null || anoInicial > anoFinal) {
            throw new IllegalArgumentException("Intervalo de anos inválido");
        }
        if (dataCorrecao == null) {
            throw new IllegalArgumentException("Data de correção obrigatória");
        }

        Map<Long, ImovelDTO> imoveis = new LinkedHashMap<>();
        String sql = "{call DBO_CCM_PESSOAS.SP_ANALISES_IMOVEIS(?,?,?,?,?)}";
        try (Connection connection = dataSource.getConnection();
             CallableStatement call = connection.prepareCall(sql)) {
            call.setInt(1, anoInicial);
            call.setInt(2, anoFinal);
            call.setDate(3, java.sql.Date.valueOf(dataCorrecao));
            call.setLong(4, pessoasCdUnico);
            call.registerOutParameter(5, Types.REF_CURSOR);
            call.execute();

            try (ResultSet rs = (ResultSet) call.getObject(5)) {
                while (rs.next()) {
                    long id = rs.getLong("IMOVEL");
                    if (rs.wasNull()) continue;
                    ImovelDTO imovel = imoveis.get(id);
                    if (imovel == null) {
                        imovel = new ImovelDTO();
                        imovel.setImovel(id);
                        imovel.setTipoLogradouro(rs.getString("TIPO_LOGRADOURO"));
                        imovel.setLogradouro(rs.getString("LOGRADOURO"));
                        imovel.setNumero(rs.getString("NUMERO"));
                        imovel.setComplemento(rs.getString("COMPLEMENTO"));
                        imovel.setBairro(rs.getString("BAIRRO"));
                        imovel.setCidade(rs.getString("CIDADE"));
                        imovel.setUf(rs.getString("UF"));
                        imovel.setCep(rs.getString("CEP"));
                        imoveis.put(id, imovel);
                    }
                    int tipo = rs.getInt("TIPO_DIVIDA");
                    if (!rs.wasNull()) {
                        ImovelDividaDTO divida = new ImovelDividaDTO();
                        divida.setTipoDivida(tipo);
                        divida.setTipoDividaDescricao(rs.getString("TIPO_DIVIDA_DESCRICAO"));
                        int ano = rs.getInt("ANO_DIVIDA");
                        if (!rs.wasNull()) divida.setAnoDivida(ano);
                        divida.setValorDivida(zero(rs.getBigDecimal("VALOR_DIVIDA")));
                        divida.setCorrecao(zero(rs.getBigDecimal("CORRECAO")));
                        divida.setMulta(zero(rs.getBigDecimal("MULTA")));
                        divida.setJuros(zero(rs.getBigDecimal("JUROS")));
                        divida.setSit(rs.getString("SIT"));
                        divida.setTotal(zero(rs.getBigDecimal("TOTAL")));
                        imovel.getDividas().add(divida);
                        imovel.setTotal(imovel.getTotal().add(divida.getTotal()));
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Erro ao consultar imóveis e dívidas no Oracle", e);
        }
        return new ArrayList<>(imoveis.values());
    }

    private static BigDecimal zero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
