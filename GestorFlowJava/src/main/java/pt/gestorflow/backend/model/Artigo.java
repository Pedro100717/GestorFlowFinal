package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "artigos")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_artigo", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
public abstract class Artigo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version // 🛡️ Ativado na Fase 1 para evitar conflitos de stock/preço
    private Long version;

    @Column(nullable = false)
    private String nome;

    private String codigoBarras;

    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(precision = 10, scale = 2)
    private BigDecimal ultimoPrecoCusto;

    @ManyToOne(fetch = FetchType.LAZY) // 🚀 Performance: Só carrega a família se pedires
    @JoinColumn(name = "familia_id")
    private Familia familia;

    @ManyToOne(fetch = FetchType.LAZY) // 🚀 Performance: Nunca carregar o utilizador sem necessidade
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @Transient
    public boolean isMovimentaStock() {
        return this instanceof Mercadoria;
    }

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artigo artigo)) return false;
        return id != null && id.equals(artigo.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}