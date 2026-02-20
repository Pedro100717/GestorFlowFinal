package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentos_tesouraria")
@Data
public class Movimento {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataMovimento;

    @Column(nullable = false)
    private String descricao; // Ex: "Pagamento Fatura EDP", "Recebimento Cliente X"

    // Enum para garantir que é só 'CREDITO' (Entrada) ou 'DEBITO' (Saída)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimento tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    // Saldo após o movimento (para extratos bancários como no banco real)
    @Column(precision = 12, scale = 2)
    private BigDecimal saldoApos;

    @ManyToOne
    @JoinColumn(name = "conta_bancaria_id", nullable = false)
    private ContaBancaria conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

    @PrePersist
    protected void onCreate() { if (dataMovimento == null) dataMovimento = LocalDateTime.now(); }

    public enum TipoMovimento {
        CREDITO, // Entra dinheiro (+)
        DEBITO   // Sai dinheiro (-)
    }
}