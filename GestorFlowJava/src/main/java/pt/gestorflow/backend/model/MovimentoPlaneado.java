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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @Column(nullable = false, name = "valor_base")
    private BigDecimal valorBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrequenciaMovimento frequencia;

    @Column(nullable = false, name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "data_ultimo_processamento")
    private LocalDate dataUltimoProcessamento;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "movimentos_planeados_excecoes", joinColumns = @JoinColumn(name = "movimento_planeado_id"))
    @Column(name = "data_excecao")
    private java.util.List<LocalDate> datasIgnoradas = new java.util.ArrayList<>();

    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    // --- MÉTODOS MATEMÁTICOS ADAPTADOS (Puro Cash Flow, sem IVA) ---
    public BigDecimal getValorComIva() {
        // Sem IVA, o valor planeado final é o próprio valor base projetado
        return this.valorBase != null ? this.valorBase : BigDecimal.ZERO;
    }

    public BigDecimal getValorIva() {
        // Previsões de tesouraria pura não retêm ou somam IVA
        return BigDecimal.ZERO;
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