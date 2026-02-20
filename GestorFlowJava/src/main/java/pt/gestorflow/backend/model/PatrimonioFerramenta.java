package pt.gestorflow.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "patrimonio_ferramentas")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatrimonioFerramenta extends Patrimonio {

    private String numeroSerie;
    private String estadoConservacao; // "Novo", "Usado", "Avariado"
}