package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // <--- Importatório
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendas")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório para fundir com a auditoria
public class Venda extends Auditable { // <--- Escudo de Auditoria Ativado

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🛡️ Tempo de Negócio: O momento em que a venda efetivamente ocorreu
    @Column(nullable = false)
    private LocalDateTime dataVenda;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal quantidade;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precoUnitario; // Excelente decisão de arquitetura guardar a "fotografia" do preço aqui!

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

    @ManyToOne
    @JoinColumn(name = "conta_bancaria_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ContaBancaria contaBancaria;

    // Renomeado para onPrePersist para evitar conflitos de ciclo de vida
    @PrePersist
    protected void onPrePersist() {
        if (dataVenda == null) dataVenda = LocalDateTime.now();
    }
}