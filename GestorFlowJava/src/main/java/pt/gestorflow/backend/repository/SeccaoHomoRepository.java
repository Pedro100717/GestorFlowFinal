package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.SeccaoHomo;
import java.util.List;

public interface SeccaoHomoRepository extends JpaRepository<SeccaoHomo, Long> {
    // Para listar nas "comboboxes" do frontend
    List<SeccaoHomo> findAllByUtilizadorId(Long utilizadorId);
}