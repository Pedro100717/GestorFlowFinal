package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Familia;

import java.util.List;

public interface FamiliaRepository extends JpaRepository<Familia, Long> {
    List<Familia> findByUtilizadorId(Long utilizadorId);
}
