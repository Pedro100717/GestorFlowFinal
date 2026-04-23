package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarefas")
@Getter
@Setter
public class Tarefa extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🛡️ Dica Industrial: Títulos devem ter um limite razoável para a UI não quebrar
    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTarefa estado = EstadoTarefa.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadeTarefa prioridade = PrioridadeTarefa.NORMAL;

    private LocalDate dataLimite;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    // 🚀 Performance: Otimizado com LAZY. Essencial para quadros Kanban rápidos.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    // 🛡️ Jackson removido
    private Utilizador utilizador;

    public enum EstadoTarefa {
        PENDENTE, EM_CURSO, CONCLUIDA, CANCELADA
    }

    public enum PrioridadeTarefa {
        BAIXA, NORMAL, ALTA, URGENTE
    }

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tarefa tarefa)) return false;
        return id != null && id.equals(tarefa.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}