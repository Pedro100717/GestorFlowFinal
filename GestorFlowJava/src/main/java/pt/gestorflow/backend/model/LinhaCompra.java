package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "linhas_compra")
@Getter
@Setter
public class LinhaCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A ligação de volta ao Cabeçalho
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

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

    @Column(precision = 10, scale = 2)
    private BigDecimal totalLinha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_custo_id")
    private CentroCusto centroCusto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccao_homo_id")
    private SeccaoHomo seccaoHomo;

    @Column(name = "designacao_personalizada")
    private String designacaoPersonalizada;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LinhaCompra linhaCompra)) return false;
        return id != null && id.equals(linhaCompra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}