package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.SeccaoHomo;
import java.util.List;
import java.util.Optional;

public interface SeccaoHomoRepository extends JpaRepository<SeccaoHomo, Long> {

    List<SeccaoHomo> findAllByUtilizadorId(Long utilizadorId);

    Optional<SeccaoHomo> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    // 🚀 OTIMIZAÇÃO: Busca em Lote (Bulk Fetching) para evitar N+1 Queries
    List<SeccaoHomo> findAllByIdInAndUtilizadorId(List<Long> ids, Long utilizadorId);

    boolean existsByCodigoAndUtilizadorId(String codigo, Long utilizadorId);
}