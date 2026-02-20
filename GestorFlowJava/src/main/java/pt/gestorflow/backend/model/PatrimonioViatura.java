package pt.gestorflow.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Entity
@Table(name = "patrimonio_viaturas")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatrimonioViatura extends Patrimonio {

    private String matricula;
    private String marca;
    private String modelo;

    private LocalDate validadeSeguro;
    private LocalDate proximaInspecao;
}