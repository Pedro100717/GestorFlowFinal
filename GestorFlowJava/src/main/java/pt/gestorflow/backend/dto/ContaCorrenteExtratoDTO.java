package pt.gestorflow.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ContaCorrenteExtratoDTO {
    private LocalDateTime dataMovimento;
    private String tipoDocumento;
    private String descricao;
    private BigDecimal debito;
    private BigDecimal credito;
    private BigDecimal saldoAcumulado;

    // Construtores, Getters e Setters
    public ContaCorrenteExtratoDTO() {}

    public ContaCorrenteExtratoDTO(LocalDateTime dataMovimento, String tipoDocumento, String descricao, BigDecimal debito, BigDecimal credito, BigDecimal saldoAcumulado) {
        this.dataMovimento = dataMovimento;
        this.tipoDocumento = tipoDocumento;
        this.descricao = descricao;
        this.debito = debito;
        this.credito = credito;
        this.saldoAcumulado = saldoAcumulado;
    }

    public LocalDateTime getDataMovimento() { return dataMovimento; }
    public void setDataMovimento(LocalDateTime dataMovimento) { this.dataMovimento = dataMovimento; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getDebito() { return debito; }
    public void setDebito(BigDecimal debito) { this.debito = debito; }
    public BigDecimal getCredito() { return credito; }
    public void setCredito(BigDecimal credito) { this.credito = credito; }
    public BigDecimal getSaldoAcumulado() { return saldoAcumulado; }
    public void setSaldoAcumulado(BigDecimal saldoAcumulado) { this.saldoAcumulado = saldoAcumulado; }
}