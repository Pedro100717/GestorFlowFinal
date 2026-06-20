package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    // O total global da fatura (Soma de todas as linhas)
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

    // --- Relações Master-Detail ---

    // 🚀 A MÁGICA DO JPA: Cascade.ALL garante que ao gravar a compra, grava as linhas juntas
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinhaCompra> linhas = new ArrayList<>();

    // --- Relações do Cabeçalho ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_bancaria_id")
    private ContaBancaria contaBancaria;

    // --- Métodos Utilitários (Obrigatório para sincronizar relações bidirecionais) ---

    public void addLinha(LinhaCompra linha) {
        linhas.add(linha);
        linha.setCompra(this);
    }

    public void removeLinha(LinhaCompra linha) {
        linhas.remove(linha);
        linha.setCompra(null);
    }

    @PrePersist
    protected void onPrePersist() {
        if (dataCompra == null) {
            dataCompra = LocalDate.now();
        }
        if (valorPago == null) {
            valorPago = BigDecimal.ZERO;
        }
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