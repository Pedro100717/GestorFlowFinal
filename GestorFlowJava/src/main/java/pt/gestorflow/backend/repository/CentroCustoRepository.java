package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.CentroCusto;
import java.util.List;

public interface CentroCustoRepository extends JpaRepository<CentroCusto, Long> {
    // Para listar nas "comboboxes" do frontend
    List<CentroCusto> findAllByUtilizadorId(Long utilizadorId);
}