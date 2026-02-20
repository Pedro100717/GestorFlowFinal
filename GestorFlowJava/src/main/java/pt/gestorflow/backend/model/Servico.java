package pt.gestorflow.backend.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@DiscriminatorValue("SERVICO")
@Data
@EqualsAndHashCode(callSuper = true)
public class Servico extends Artigo {

    // Serviços não têm stock, mas podem ter, por exemplo:
    // private Integer duracaoMinutosEstimada;
}