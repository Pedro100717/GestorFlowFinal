package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "contas_bancarias")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ContaBancaria extends Auditable{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome; // Ex: "Santander", "Caixa Física"

    private String iban;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO; // Saldo atual

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;
}