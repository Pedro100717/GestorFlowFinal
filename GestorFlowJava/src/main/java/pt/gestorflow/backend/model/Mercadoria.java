package pt.gestorflow.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("MERCADORIA")
@Getter
@Setter
public class Mercadoria extends Artigo {

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal stockAtual = BigDecimal.ZERO;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE
    // Como Artigo já define equals/hashCode baseados no ID,
    // não precisamos de repetir a lógica aqui.
    // O Lombok @EqualsAndHashCode(callSuper = true) foi removido
    // para evitar que o stock atual (que muda sempre) influencie o Hash.
}