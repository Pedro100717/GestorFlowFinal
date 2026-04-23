package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Objects;

@Entity
@Table(name = "centro_custo")
@Getter
@Setter
public class CentroCusto extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true) // 🛡️ Boa prática: códigos de centro de custo costumam ser únicos
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    // 🛡️ Jackson removido: A entidade já não sabe o que é um JSON.
    private Utilizador utilizador;

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CentroCusto that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        // Retornamos um valor constante para garantir que a entidade
        // se mantém estável em coleções antes e depois de persistida.
        return getClass().hashCode();
    }
}