package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.gestorflow.backend.model.MovimentoPlaneado;

import java.util.List;

@Repository
public interface MovimentoPlaneadoRepository extends JpaRepository<MovimentoPlaneado, Long> {

    // O motor matemático vai usar isto para carregar todas as regras ativas do gestor
    List<MovimentoPlaneado> findAllByUtilizadorIdAndAtivoTrue(Long utilizadorId);
}