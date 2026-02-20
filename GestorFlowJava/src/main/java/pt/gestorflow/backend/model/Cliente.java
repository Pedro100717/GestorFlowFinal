package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

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

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    // A Chave Estrangeira!
    // @JsonIgnore: Impede que, ao pedires um cliente, ele traga o utilizador inteiro (com password e tudo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }
}