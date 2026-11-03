package br.com.cc.pessoas.dto.imovel;

import java.math.BigDecimal;

public class ImovelDividaDTO {
    private Integer tipoDivida;
    private String tipoDividaDescricao;
    private Integer anoDivida;
    private BigDecimal valorDivida = BigDecimal.ZERO;
    private BigDecimal correcao = BigDecimal.ZERO;
    private BigDecimal multa = BigDecimal.ZERO;
    private BigDecimal juros = BigDecimal.ZERO;
    private String sit;
    private BigDecimal total = BigDecimal.ZERO;

    public Integer getTipoDivida() { return tipoDivida; }
    public void setTipoDivida(Integer v) { this.tipoDivida = v; }
    public String getTipoDividaDescricao() { return tipoDividaDescricao; }
    public void setTipoDividaDescricao(String v) { this.tipoDividaDescricao = v; }
    public Integer getAnoDivida() { return anoDivida; }
    public void setAnoDivida(Integer v) { this.anoDivida = v; }
    public BigDecimal getValorDivida() { return valorDivida; }
    public void setValorDivida(BigDecimal v) { this.valorDivida = v; }
    public BigDecimal getCorrecao() { return correcao; }
    public void setCorrecao(BigDecimal v) { this.correcao = v; }
    public BigDecimal getMulta() { return multa; }
    public void setMulta(BigDecimal v) { this.multa = v; }
    public BigDecimal getJuros() { return juros; }
    public void setJuros(BigDecimal v) { this.juros = v; }
    public String getSit() { return sit; }
    public void setSit(String v) { this.sit = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { this.total = v; }
}
