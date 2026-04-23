package pt.gestorflow.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "patrimonio_viaturas")
@Getter
@Setter
public class PatrimonioViatura extends Patrimonio {

    // 🛡️ Dica Industrial: Matrículas devem ser únicas e curtas para indexação rápida
    @Column(length = 20, unique = true, nullable = false)
    private String matricula;

    @Column(length = 50)
    private String marca;

    @Column(length = 100)
    private String modelo;

    private LocalDate validadeSeguro;

    private LocalDate proximaInspecao;

    // 🛡️ REMOVIDO: @EqualsAndHashCode(callSuper = true) e import lombok.Data;
    // A identidade é garantida pelo ID na classe mãe (Patrimonio).
}