package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity // Diz ao Spring: "Isto é uma tabela na base de dados"
@Table(name = "utilizadores") // Nome da tabela no Postgres
@Data // O Lombok cria automaticamente os Getters, Setters, ToString, etc.
@NoArgsConstructor // Construtor vazio (obrigatório para JPA)
@AllArgsConstructor // Construtor com tudo
public class Utilizador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Equivalente ao AUTO_INCREMENT / SERIAL
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nomeUtilizador;

    @Column(nullable = false)
    private String senha; // Em produção, isto será o hash, não a password limpa

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    private Boolean verificado = false;

    private String codigoVerificacao;

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    // Antes de guardar na BD pela primeira vez, define a data atual
    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }
}