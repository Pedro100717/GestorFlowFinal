package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orcamentos")
@Data
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    private LocalDate dataValidade; // Até quando o preço é garantido

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrcamento estado = EstadoOrcamento.RASCUNHO;

    @Column(columnDefinition = "TEXT")
    private String notas;

    // Totais do Cabeçalho
    @Column(precision = 12, scale = 2)
    private BigDecimal totalCusto = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalSemIva = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalComIva = BigDecimal.ZERO;

    // Relações
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

    // Cascade: Se apagares o orçamento, apaga as linhas.
    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinhaOrcamento> linhas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
        if (dataValidade == null) dataValidade = LocalDate.now().plusDays(30); // 30 dias por defeito
    }

    public enum EstadoOrcamento {
        RASCUNHO, ENVIADO, APROVADO, REJEITADO, CONVERTIDO_VENDA
    }
}