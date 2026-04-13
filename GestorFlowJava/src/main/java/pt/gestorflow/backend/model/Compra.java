package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // <--- Não esquecer o import!
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compras")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório para cruzar com o Auditable
public class Compra extends Auditable { // <--- Herda o Carimbo de Auditoria

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🛡️ INTOCÁVEL: Tempo de Negócio (O utilizador pode retrodatar isto!)
    @Column(nullable = false)
    private LocalDateTime dataCompra;

    private String numeroFaturaFornecedor;

    @Column(nullable = false)
    private String designacao;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal quantidade;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precoUnitario; // Preço Base (Sem IVA)

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

    @ManyToOne
    @JoinColumn(name = "centro_custo_id")
    private CentroCusto centroCusto;

    @ManyToOne
    @JoinColumn(name = "seccao_homo_id")
    private SeccaoHomo seccaoHomo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

    @ManyToOne
    @JoinColumn(name = "conta_bancaria_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ContaBancaria contaBancaria;

    // Mantemos isto como "Rede de Segurança" caso a data de negócio venha vazia do frontend
    @PrePersist
    protected void onPrePersist() {
        if (dataCompra == null) {
            dataCompra = LocalDateTime.now();
        }
    }
}