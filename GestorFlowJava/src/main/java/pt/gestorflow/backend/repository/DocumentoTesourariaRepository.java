package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.DocumentoTesouraria;
import pt.gestorflow.backend.model.EstadoPagamento;
import java.util.List;
import java.util.Optional;

public interface DocumentoTesourariaRepository extends JpaRepository<DocumentoTesouraria, Long> {
    List<DocumentoTesouraria> findAllByUtilizadorIdAndEstadoPagamentoIn(Long utilizadorId, List<EstadoPagamento> estados);
    Optional<DocumentoTesouraria> findByIdAndUtilizadorId(Long id, Long utilizadorId);
}