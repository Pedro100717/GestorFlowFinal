package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "compras")
@Getter
@Setter
public class Compra extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataCompra;

    // 🚀 O MOTOR DO SIMULADOR DE TESOURARIA
    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    private String numeroFaturaFornecedor;

    @Column(nullable = false)
    private String designacao;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal quantidade;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precoUnitario;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    // O que já foi efetivamente pago ao fornecedor
    @Column(precision = 10, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO;

    @Column(name = "data_prevista_pagamento")
    private LocalDate dataPrevistaPagamento;

    @Column(name = "plano_origem_id")
    private Long planoOrigemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "estado_pagamento")
    private EstadoPagamento estadoPagamento = EstadoPagamento.PENDENTE;

    // --- Relações ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tx_iva_id", nullable = false)
    private TxIva taxaIva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artigo_id", nullable = false)
    private Artigo artigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_custo_id")
    private CentroCusto centroCusto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccao_homo_id")
    private SeccaoHomo seccaoHomo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_bancaria_id")
    private ContaBancaria contaBancaria;

    @PrePersist
    protected void onPrePersist() {
        if (dataCompra == null) {
            dataCompra = LocalDate.now();
        }
        if (valorPago == null) {
            valorPago = BigDecimal.ZERO;
        }
        // 🛡️ FALLBACK: Garante que faturas antigas não partem o simulador
        if (dataVencimento == null) {
            dataVencimento = dataCompra;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Compra compra)) return false;
        return id != null && id.equals(compra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}