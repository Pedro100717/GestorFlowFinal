package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_tesouraria")
@Getter
@Setter
public class DocumentoTesouraria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String descricao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentoPlaneado tipo;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO;
    @Column(nullable = false)
    private LocalDateTime dataEmissao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "estado_pagamento")
    private EstadoPagamento estadoPagamento = EstadoPagamento.PENDENTE;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;
}