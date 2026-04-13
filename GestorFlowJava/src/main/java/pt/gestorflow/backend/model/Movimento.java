package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // <--- Não esquecer!
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentos_tesouraria")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório por causa da herança
public class Movimento extends Auditable { // <--- Escudo de Auditoria Ativado

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🛡️ Tempo de Negócio: O dia em que o dinheiro efetivamente mexeu no banco
    @Column(nullable = false)
    private LocalDateTime dataMovimento;

    @Column(nullable = false)
    private String descricao; // Ex: "Pagamento Fatura EDP", "Recebimento Cliente X"

    // Enum para garantir que é só 'CREDITO' (Entrada) ou 'DEBITO' (Saída)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimento tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    // Saldo após o movimento (para extratos bancários como no banco real)
    @Column(precision = 12, scale = 2)
    private BigDecimal saldoApos;

    @ManyToOne
    @JoinColumn(name = "conta_bancaria_id", nullable = false)
    private ContaBancaria conta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

    public enum TipoMovimento {
        CREDITO, // Entra dinheiro (+)
        DEBITO   // Sai dinheiro (-)
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cliente cliente;

    // Rede de Segurança (Mudei o nome para não haver risco de conflito interno do JPA)
    @PrePersist
    protected void onPrePersist() {
        if (dataMovimento == null) {
            dataMovimento = LocalDateTime.now();
        }
    }
}