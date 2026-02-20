package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compras")
@Data
public class Compra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataCompra;

    private String numeroFaturaFornecedor;

    @Column(nullable = false)
    private String designacao;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal quantidade;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precoUnitario; // Preço Base (Sem IVA)

    // --- NOVO: Lógica de IVA ---
    @ManyToOne
    @JoinColumn(name = "tx_iva_id", nullable = false)
    private TxIva taxaIva;

    @Column(precision = 10, scale = 2)
    private BigDecimal total; // Total Final (Com IVA)

    // --- Relações ---
    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @ManyToOne
    @JoinColumn(name = "artigo_id", nullable = false)
    private Artigo artigo;

    // Analítica
    @ManyToOne @JoinColumn(name = "centro_custo_id")
    private CentroCusto centroCusto;

    @ManyToOne @JoinColumn(name = "seccao_homo_id")
    private SeccaoHomo seccaoHomo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

    @PrePersist
    protected void onCreate() { if (dataCompra == null) dataCompra = LocalDateTime.now(); }
}