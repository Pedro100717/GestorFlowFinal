package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentos_stock")
@Getter
@Setter
public class MovimentoStock extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataMovimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentoStock tipo;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal quantidade;

    @Column(nullable = false)
    private String motivo;

    @Column(precision = 10, scale = 3)
    private BigDecimal stockAposMovimento;

    // 🛡️ Otimização LAZY: Essencial para relatórios de histórico de stock
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mercadoria_id", nullable = false)
    private Mercadoria mercadoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    public enum TipoMovimentoStock {
        ENTRADA,
        SAIDA
    }

    @PrePersist
    protected void onPrePersist() {
        if (dataMovimento == null) {
            dataMovimento = LocalDateTime.now();
        }
    }

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovimentoStock that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}