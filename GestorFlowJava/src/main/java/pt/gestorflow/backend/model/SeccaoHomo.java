package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seccao_homo")
@Getter
@Setter
public class SeccaoHomo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    // 🛡️ Dica Industrial: Códigos analíticos devem ser únicos e curtos para indexação
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    // 🛡️ Jackson removido
    private Utilizador utilizador;

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeccaoHomo that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}