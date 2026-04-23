package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tx_iva")
@Getter
@Setter
public class TxIva extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🛡️ Dica Industrial: Descrições devem ser únicas para evitar duplicação no dropdown
    @Column(nullable = false, unique = true, length = 50)
    private String descricao;

    // 🛡️ Precisão essencial para percentagens (ex: 23.00)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal valor;

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TxIva that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}