package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Familia;

import java.util.List;
import java.util.Optional; // <--- NÃO ESQUECER ESTE IMPORT

public interface FamiliaRepository extends JpaRepository<Familia, Long> {

    List<Familia> findByUtilizadorId(Long utilizadorId);

    // 🛡️ A TRANCA DE SEGURANÇA OBRIGATÓRIA
    Optional<Familia> findByIdAndUtilizadorId(Long id, Long utilizadorId);
}