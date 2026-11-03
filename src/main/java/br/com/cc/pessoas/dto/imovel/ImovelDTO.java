package br.com.cc.pessoas.dto.imovel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ImovelDTO {
    private Long imovel;
    private String tipoLogradouro;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String cep;
    private BigDecimal total = BigDecimal.ZERO;
    private List<ImovelDividaDTO> dividas = new ArrayList<>();

    public Long getImovel() { return imovel; }
    public void setImovel(Long v) { this.imovel = v; }
    public String getTipoLogradouro() { return tipoLogradouro; }
    public void setTipoLogradouro(String v) { this.tipoLogradouro = v; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String v) { this.logradouro = v; }
    public String getNumero() { return numero; }
    public void setNumero(String v) { this.numero = v; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String v) { this.complemento = v; }
    public String getBairro() { return bairro; }
    public void setBairro(String v) { this.bairro = v; }
    public String getCidade() { return cidade; }
    public void setCidade(String v) { this.cidade = v; }
    public String getUf() { return uf; }
    public void setUf(String v) { this.uf = v; }
    public String getCep() { return cep; }
    public void setCep(String v) { this.cep = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { this.total = v; }
    public List<ImovelDividaDTO> getDividas() { return dividas; }
    public void setDividas(List<ImovelDividaDTO> v) { this.dividas = v; }
}
