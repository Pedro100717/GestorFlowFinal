package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "patrimonio")
@Inheritance(strategy = InheritanceType.JOINED) // CRÍTICO: Cria tabelas separadas mas ligadas por ID
@Data
// Configuração para o JSON saber qual filho criar quando recebe dados do Frontend
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipoPatrimonio")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PatrimonioViatura.class, name = "VIATURA"),
        @JsonSubTypes.Type(value = PatrimonioImovel.class, name = "IMOVEL"),
        @JsonSubTypes.Type(value = PatrimonioFerramenta.class, name = "FERRAMENTA")
})
public abstract class Patrimonio {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome; // Ex: "Carrinha Mercedes", "Escritório Lisboa"

    private LocalDate dataAquisicao;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorAquisicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;
}