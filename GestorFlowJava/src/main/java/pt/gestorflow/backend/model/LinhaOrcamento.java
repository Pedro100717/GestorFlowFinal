package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // <--- Não esquecer!
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "orcamentos_linhas")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório para fundir com a classe mãe
public class LinhaOrcamento extends Auditable { // <--- Controlo anti-fraude ativado

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "orcamento_id", nullable = false)
    @JsonIgnore // Evita loop infinito no JSON
    private Orcamento orcamento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "artigo_id")
    private Artigo artigo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tx_iva_id")
    private TxIva taxaIva;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal quantidade;

    // CRÍTICO: Tirar uma "fotografia" ao preço de custo no momento em que se faz o orçamento
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precoCustoUnitario;

    // Margem aplicada nesta linha específica (em percentagem)
    @Column(precision = 5, scale = 2)
    private BigDecimal margemLucroPercentual;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precoVendaUnitario; // Calculado: Custo + Margem

    // Totais da Linha
    @Column(precision = 12, scale = 2)
    private BigDecimal totalLinhaSemIva;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalLinhaComIva;
}