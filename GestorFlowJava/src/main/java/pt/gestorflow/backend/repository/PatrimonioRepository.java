package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.gestorflow.backend.model.Patrimonio;

import java.util.Optional;

public interface PatrimonioRepository extends JpaRepository<Patrimonio, Long> {

    Optional<Patrimonio> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    // Agora devolve uma Página e exige o Pageable!
    Page<Patrimonio> findAllByUtilizadorIdAndAtivoTrue(Long utilizadorId, Pageable pageable);

    // 🚀 A magia acontece aqui: Ensinamos o JPA a procurar diretamente na subclasse
    @Query("SELECT COUNT(v) > 0 FROM PatrimonioViatura v WHERE v.matricula = :matricula AND v.utilizador.id = :utilizadorId")
    boolean existsByMatriculaAndUtilizadorId(@Param("matricula") String matricula, @Param("utilizadorId") Long utilizadorId);
}