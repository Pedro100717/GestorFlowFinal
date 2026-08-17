package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports_suporte")
@Getter
@Setter
public class ReportSuporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "pagina_origem")
    private String paginaOrigem;

    @Column(name = "email_utilizador")
    private String emailUtilizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id")
    private Utilizador utilizador;

    @Column(nullable = false, length = 20)
    private String estado = "ABERTO";

    @Column(name = "data_criacao_sistema", updatable = false)
    private LocalDateTime dataCriacaoSistema = LocalDateTime.now();
}