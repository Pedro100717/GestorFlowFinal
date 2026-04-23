package pt.gestorflow.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "patrimonio_imoveis")
@Getter
@Setter
public class PatrimonioImovel extends Patrimonio {

    @Column(columnDefinition = "TEXT")
    private String morada;

    @Column(length = 50)
    private String artigoMatricial; // 🛡️ Finanças (Limitado para indexação)

    @Column(length = 50)
    private String tipo; // "Urbano", "Rústico"

    // 🛡️ REMOVIDO: @EqualsAndHashCode(callSuper = true) e @Data
    // A identidade (equals/hashCode) é garantida pelo ID na classe mãe (Patrimonio).
}