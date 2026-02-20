package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Patrimonio;
import java.util.List;

public interface PatrimonioRepository extends JpaRepository<Patrimonio, Long> {
    // Traz TUDO misturado (Viaturas, Imóveis, etc.)
    List<Patrimonio> findAllByUtilizadorId(Long utilizadorId);
}