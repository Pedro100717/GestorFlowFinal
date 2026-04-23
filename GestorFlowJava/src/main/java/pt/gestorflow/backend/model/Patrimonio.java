package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "patrimonio")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Patrimonio extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private LocalDate dataAquisicao;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorAquisicao;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    // 🛡️ Jackson removido: @JsonIgnore exterminado.
    private Utilizador utilizador;

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patrimonio that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}