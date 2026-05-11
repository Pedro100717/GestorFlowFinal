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
    private TipoMovimentoPlaneado tipo; // ENTRADA ou SAIDA

    @Column(nullable = false, name = "valor_base")
    private BigDecimal valorBase;

    // 🚀 LIGAÇÃO REAL AO IVA (Única obrigatória para os cálculos)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tx_iva_id", nullable = false)
    private TxIva taxaIva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrequenciaMovimento frequencia;

    @Column(nullable = false, name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "data_ultimo_processamento")
    private LocalDate dataUltimoProcessamento;

    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    // --- MÉTODOS MATEMÁTICOS ADAPTADOS ---
    public BigDecimal getValorComIva() {
        if (valorBase == null || taxaIva == null || taxaIva.getValor() == null) return valorBase;
        BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE);
        return valorBase.multiply(fatorIva);
    }

    public BigDecimal getValorIva() {
        if (valorBase == null || taxaIva == null || taxaIva.getValor() == null) return BigDecimal.ZERO;
        return valorBase.multiply(taxaIva.getValor().divide(BigDecimal.valueOf(100)));
    }

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