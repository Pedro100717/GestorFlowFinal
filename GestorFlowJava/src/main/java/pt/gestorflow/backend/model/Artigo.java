package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "artigos")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // Tudo na mesma tabela 'artigos'
@DiscriminatorColumn(name = "tipo_artigo", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class Artigo extends Auditable{ // <--- Agora é ABSTRACT

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String codigoBarras;

    @Column(precision = 10, scale = 2)
    private BigDecimal preco; // Preço de Venda

    @Column(precision = 10, scale = 2)
    private BigDecimal ultimoPrecoCusto;

    @ManyToOne
    @JoinColumn(name = "familia_id")
    private Familia familia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

    @Transient
    public boolean getMovimentaStock() {
        return this instanceof Mercadoria;
    }

}