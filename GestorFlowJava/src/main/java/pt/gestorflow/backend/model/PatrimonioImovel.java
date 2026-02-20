package pt.gestorflow.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "patrimonio_imoveis")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatrimonioImovel extends Patrimonio {

    private String morada;
    private String artigoMatricial; // Finanças
    private String tipo; // "Urbano", "Rústico"
}