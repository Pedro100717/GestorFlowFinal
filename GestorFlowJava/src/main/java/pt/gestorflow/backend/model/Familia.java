package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "familias")
@Getter
@Setter
public class Familia extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    // 🛡️ Jackson removido: A entidade agora é agnóstica em relação ao JSON.
    private Utilizador utilizador;

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Familia familia)) return false;
        return id != null && id.equals(familia.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}