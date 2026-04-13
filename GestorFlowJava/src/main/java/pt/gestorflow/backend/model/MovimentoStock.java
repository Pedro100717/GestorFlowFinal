package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // <--- Importatório obrigatório
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentos_stock")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório por causa da herança
public class MovimentoStock extends Auditable { // <--- Escudo de Auditoria Ativado

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🛡️ Tempo de Negócio: Quando o stock efetivamente mexeu
    @Column(nullable = false)
    private LocalDateTime dataMovimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentoStock tipo;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal quantidade;

    @Column(nullable = false)
    private String motivo; // Ex: "Quebra", "Acerto de Inventário", "Oferta Comercial"

    // O saldo do artigo no exato momento após este movimento (ótimo para rastreabilidade)
    @Column(precision = 10, scale = 3)
    private BigDecimal stockAposMovimento;

    // Relacionamos DIRETAMENTE com Mercadoria (pois Servicos não têm stock)
    @ManyToOne
    @JoinColumn(name = "mercadoria_id", nullable = false)
    private Mercadoria mercadoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

    public enum TipoMovimentoStock {
        ENTRADA,
        SAIDA
    }

    // A nossa rede de segurança de negócio, renomeada para não causar conflitos
    @PrePersist
    protected void onPrePersist() {
        if (dataMovimento == null) {
            dataMovimento = LocalDateTime.now();
        }
    }
}