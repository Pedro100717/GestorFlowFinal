package pt.gestorflow.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "patrimonio_ferramentas")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PatrimonioFerramenta extends Patrimonio {

    private String numeroSerie;
    private String estadoConservacao; // "Novo", "Usado", "Avariado"
}