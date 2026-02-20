package pt.gestorflow.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("MERCADORIA") // O valor que fica na coluna 'tipo_artigo'
@Data
@EqualsAndHashCode(callSuper = true)
public class Mercadoria extends Artigo {

    @Column(precision = 10, scale = 3)
    private BigDecimal stockAtual = BigDecimal.ZERO;

    @Column(precision = 10, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    // Podes adicionar mais coisas específicas aqui, ex: peso, localização no armazém...
}