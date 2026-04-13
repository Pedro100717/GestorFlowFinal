package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Cliente extends Auditable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String nif; // Não pus 'unique' aqui no Java para controlar via código se necessário

    private String email;

    private String telefone;

    @Column(columnDefinition = "TEXT")
    private String morada;

    @Column(columnDefinition = "TEXT")
    private String anotacoes;

    // A Chave Estrangeira!
    // @JsonIgnore: Impede que, ao pedires um cliente, ele traga o utilizador inteiro (com password e tudo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

}