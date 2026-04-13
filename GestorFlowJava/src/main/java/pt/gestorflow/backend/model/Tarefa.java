package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // <--- Importatório obrigatório
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarefas")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório para fundir com o Auditable
public class Tarefa extends Auditable { // <--- Escudo de Auditoria Ativado

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTarefa estado = EstadoTarefa.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadeTarefa prioridade = PrioridadeTarefa.NORMAL;

    private LocalDate dataLimite; // Deadline

    // 🛡️ Tempo de Negócio: Quando o trabalho foi efetivamente terminado
    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    // --- Relações ---
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

    public enum EstadoTarefa {
        PENDENTE, EM_CURSO, CONCLUIDA, CANCELADA
    }

    public enum PrioridadeTarefa {
        BAIXA, NORMAL, ALTA, URGENTE
    }
}