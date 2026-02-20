package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Orcamento;

import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    // Listagem paginada e filtrada por utilizador
    Page<Orcamento> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    // Buscar um orçamento específico garantindo que pertence ao utilizador
    Optional<Orcamento> findByIdAndUtilizadorId(Long id, Long utilizadorId);
}