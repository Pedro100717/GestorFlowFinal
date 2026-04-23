package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "linhas_venda")
@Getter
@Setter
public class LinhaVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A ligação de volta ao Cabeçalho
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artigo_id", nullable = false)
    private Artigo artigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tx_iva_id", nullable = false)
    private TxIva taxaIva;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal quantidade;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precoUnitario;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalLinhaSemIva;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalLinhaComIva;

    @Column(name = "designacao_personalizada", length = 255)
    private String designacaoPersonalizada;

    // Identidade blindada
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LinhaVenda linha)) return false;
        return id != null && id.equals(linha.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}