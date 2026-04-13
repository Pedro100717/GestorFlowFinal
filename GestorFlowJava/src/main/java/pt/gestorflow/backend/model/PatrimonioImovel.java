package pt.gestorflow.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "patrimonio_imoveis")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PatrimonioImovel extends Patrimonio {

    private String morada;
    private String artigoMatricial; // Finanças
    private String tipo; // "Urbano", "Rústico"
}