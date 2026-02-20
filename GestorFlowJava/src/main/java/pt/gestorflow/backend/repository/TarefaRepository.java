package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Tarefa;

import java.util.List;
import java.util.Optional;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    // Listagem base
    Page<Tarefa> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    // Listagem filtrada por estado (ex: Dashboard de tarefas pendentes)
    List<Tarefa> findAllByUtilizadorIdAndEstado(Long utilizadorId, Tarefa.EstadoTarefa estado);

    // Ver histórico de tarefas de um cliente específico
    List<Tarefa> findAllByUtilizadorIdAndClienteId(Long utilizadorId, Long clienteId);

    // Segurança
    Optional<Tarefa> findByIdAndUtilizadorId(Long id, Long utilizadorId);
}