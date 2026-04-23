package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "orcamento_linhas")
@Getter
@Setter
public class LinhaOrcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🛡️ Relação Forte com o "Mestre"
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artigo_id", nullable = false)
    private Artigo artigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tx_iva_id", nullable = false)
    private TxIva taxaIva;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantidade;

    // 🛡️ Snapshot de Preços: Guardamos os valores no momento da criação
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoCustoUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVendaUnitario;

    @Column(precision = 10, scale = 2)
    private BigDecimal margemLucroPercentual;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalLinhaSemIva;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalLinhaComIva;

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LinhaOrcamento that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}