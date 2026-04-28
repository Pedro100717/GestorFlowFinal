package pt.gestorflow.backend.dto;

import java.math.BigDecimal;

public class ContaCorrenteResumoDTO {

    private Long clienteId;
    private Long fornecedorId;

    // 🚀 O Angular precisa EXATAMENTE destes nomes para a tabela funcionar:
    private String nome;
    private BigDecimal totalFaturado; // Para clientes = Faturado. Para fornecedores = Comprado.
    private BigDecimal totalPago;
    private BigDecimal saldoPendente;

    public ContaCorrenteResumoDTO() {}

    public ContaCorrenteResumoDTO(Long clienteId, Long fornecedorId, String nome, BigDecimal totalFaturado, BigDecimal totalPago, BigDecimal saldoPendente) {
        this.clienteId = clienteId;
        this.fornecedorId = fornecedorId;
        this.nome = nome;
        this.totalFaturado = totalFaturado != null ? totalFaturado : BigDecimal.ZERO;
        this.totalPago = totalPago != null ? totalPago : BigDecimal.ZERO;
        this.saldoPendente = saldoPendente != null ? saldoPendente : BigDecimal.ZERO;
    }

    // --- GETTERS E SETTERS ---
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public Long getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(Long fornecedorId) { this.fornecedorId = fornecedorId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getTotalFaturado() { return totalFaturado; }
    public void setTotalFaturado(BigDecimal totalFaturado) { this.totalFaturado = totalFaturado; }

    public BigDecimal getTotalPago() { return totalPago; }
    public void setTotalPago(BigDecimal totalPago) { this.totalPago = totalPago; }

    public BigDecimal getSaldoPendente() { return saldoPendente; }
    public void setSaldoPendente(BigDecimal saldoPendente) { this.saldoPendente = saldoPendente; }
}