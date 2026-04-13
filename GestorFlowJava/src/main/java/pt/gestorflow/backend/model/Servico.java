package pt.gestorflow.backend.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("SERVICO")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Servico extends Artigo {

    // Serviços não têm stock, mas podem ter, por exemplo:
    // private Integer duracaoMinutosEstimada;
}