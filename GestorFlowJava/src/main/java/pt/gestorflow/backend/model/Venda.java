package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendas")
@Getter
@Setter
public class Venda extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataVenda;

    // 🚀 O MOTOR DO SIMULADOR DE TESOURARIA
    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalSemIva;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalComIva;

    // 🚀 NOVO CAMPO: O que já foi efetivamente pago
    @Column(precision = 10, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO;

    @Column(name = "data_prevista_pagamento")
    private LocalDate dataPrevistaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "estado_pagamento")
    private EstadoPagamento estadoPagamento = EstadoPagamento.PENDENTE;

    @Column(name = "plano_origem_id")
    private Long planoOrigemId;

    // ==========================================
    // 🚀 O CORAÇÃO DO NOVO ERP: MÚLTIPLAS LINHAS
    // ==========================================
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinhaVenda> linhas = new ArrayList<>();

    // ==========================================
    // 🚀 OTIMIZAÇÃO EXTREMA: RELAÇÕES LAZY
    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_custo_id")
    private CentroCusto centroCusto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccao_homo_id")
    private SeccaoHomo seccaoHomo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_bancaria_id")
    private ContaBancaria contaBancaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @PrePersist
    protected void onPrePersist() {
        if (dataVenda == null) dataVenda = LocalDate.now();
        if (valorPago == null) valorPago = BigDecimal.ZERO; // Dupla segurança
        // 🛡️ FALLBACK: Garante que faturas antigas não partem o simulador
        if (dataVencimento == null) dataVencimento = dataVenda;
    }

    // ==========================================
    // 🛡️ IDENTIDADE BLINDADA PARA O HIBERNATE
    // ==========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Venda venda)) return false;
        return id != null && id.equals(venda.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}