package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movimentos_planeados")
@Getter
@Setter
public class MovimentoPlaneado extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentoPlaneado tipo; // ENTRADA (Vendas/Receitas) ou SAIDA (Compras/Custos)

    // 🚀 RIGOR INDUSTRIAL: Valores para simulação de fluxo de caixa e impostos
    @Column(nullable = false)
    private BigDecimal valorBase;

    @Column(nullable = false)
    private BigDecimal taxaIva; // Ex: 23.0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrequenciaMovimento frequencia;

    @Column(nullable = false)
    private LocalDate dataInicio;

    @Column
    private LocalDate dataFim;

    // 🚀 O "TRIÂNGULO DOURADO" DE CONCILIAÇÃO ANALÍTICA
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_custo_id", nullable = false)
    private CentroCusto centroCusto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccao_homo_id", nullable = false)
    private SeccaoHomo seccaoHomo;

    // 🚀 LIGAÇÕES AOS PARCEIROS (Opcionais para permitir previsões genéricas)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente; // Usado quando o tipo é ENTRADA

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor; // Usado quando o tipo é SAIDA

    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    // --- MÉTODOS DE APOIO AO MOTOR MATEMÁTICO ---

    public BigDecimal getValorComIva() {
        if (valorBase == null) return BigDecimal.ZERO;
        BigDecimal taxa = taxaIva != null ? taxaIva : BigDecimal.ZERO;
        BigDecimal fatorIva = taxa.divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE);
        return valorBase.multiply(fatorIva);
    }

    public BigDecimal getValorIva() {
        if (valorBase == null) return BigDecimal.ZERO;
        BigDecimal taxa = taxaIva != null ? taxaIva : BigDecimal.ZERO;
        return valorBase.multiply(taxa.divide(BigDecimal.valueOf(100)));
    }

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovimentoPlaneado that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}