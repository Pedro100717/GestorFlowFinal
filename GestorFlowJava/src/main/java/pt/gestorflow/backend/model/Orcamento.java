package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orcamentos")
@Getter
@Setter
public class Orcamento extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataEmissao;

    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrcamento estado = EstadoOrcamento.RASCUNHO;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalCusto = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalSemIva = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalComIva = BigDecimal.ZERO;

    // 🚀 Performance: Otimizado com LAZY para evitar o problema N+1 ao listar orçamentos
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    // 🛡️ Jackson removido
    private Utilizador utilizador;

    // 🛡️ As exclusões do Lombok (@ToString.Exclude, etc.) saíram, pois já não usamos o @Data
    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinhaOrcamento> linhas = new ArrayList<>();

    @PrePersist
    protected void onPrePersist() {
        if (dataEmissao == null) dataEmissao = LocalDate.now();
        if (dataValidade == null) dataValidade = dataEmissao.plusDays(30);
    }

    public enum EstadoOrcamento {
        RASCUNHO, ENVIADO, APROVADO, REJEITADO, CONVERTIDO_VENDA
    }

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Orcamento that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}