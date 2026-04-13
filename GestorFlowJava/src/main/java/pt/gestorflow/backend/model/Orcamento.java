package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orcamentos")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório por causa da herança
public class Orcamento extends Auditable { // <--- Escudo de Auditoria Ativado

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🛡️ Tempo de Negócio: O dia que vai impresso no PDF do Orçamento
    @Column(nullable = false)
    private LocalDate dataEmissao;

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
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinhaOrcamento> linhas = new ArrayList<>();

    // Rede de segurança para os dados de negócio
    @PrePersist
    protected void onPrePersist() {
        if (dataEmissao == null) dataEmissao = LocalDate.now();
        if (dataValidade == null) dataValidade = dataEmissao.plusDays(30); // 30 dias a partir da data de emissão
    }

    public enum EstadoOrcamento {
        RASCUNHO, ENVIADO, APROVADO, REJEITADO, CONVERTIDO_VENDA
    }
}