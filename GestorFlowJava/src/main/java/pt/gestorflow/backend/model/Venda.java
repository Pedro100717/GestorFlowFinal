package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendas")
@Data
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataVenda;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal quantidade;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precoUnitario; // Guardamos o preço no momento da venda, pois o preço do artigo pode mudar no futuro!

    @Column(precision = 10, scale = 2)
    private BigDecimal totalSemIva;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalComIva;

    // --- Relações ---
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "artigo_id", nullable = false)
    private Artigo artigo;

    @ManyToOne
    @JoinColumn(name = "tx_iva_id", nullable = false)
    private TxIva taxaIva;

    // Analítica (Opcional por venda)
    @ManyToOne
    @JoinColumn(name = "centro_custo_id")
    private CentroCusto centroCusto;

    @ManyToOne
    @JoinColumn(name = "seccao_homo_id")
    private SeccaoHomo seccaoHomo;

    @Column(nullable = false)
    private String designacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

    @PrePersist
    protected void onCreate() {
        if (dataVenda == null) dataVenda = LocalDateTime.now();
    }
}