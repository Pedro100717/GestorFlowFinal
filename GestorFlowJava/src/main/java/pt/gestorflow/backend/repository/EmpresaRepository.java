package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.gestorflow.backend.model.Empresa;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    // Procura a empresa através do ID do utilizador logado
    Optional<Empresa> findByUtilizadorId(Long utilizadorId);
}