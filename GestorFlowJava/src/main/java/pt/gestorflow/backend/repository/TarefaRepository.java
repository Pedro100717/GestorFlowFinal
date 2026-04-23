package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Tarefa;

import java.util.List;
import java.util.Optional;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    // 🚀 OTIMIZAÇÃO: Traz o Cliente à boleia para evitar N+1 queries!
    @EntityGraph(attributePaths = {"cliente"})
    Page<Tarefa> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente"})
    List<Tarefa> findAllByUtilizadorIdAndEstado(Long utilizadorId, Tarefa.EstadoTarefa estado);

    @EntityGraph(attributePaths = {"cliente"})
    List<Tarefa> findAllByUtilizadorIdAndClienteId(Long utilizadorId, Long clienteId);

    // Otimização para quando abres o detalhe
    @EntityGraph(attributePaths = {"cliente"})
    Optional<Tarefa> findByIdAndUtilizadorId(Long id, Long utilizadorId);
}